/*
 *  Author : jinkang zhang / jk0218.zhang@samsung.com
 *  Date : 2018-07-04
 */
import physicalgraph.zigbee.clusters.iaszone.ZoneStatus
import physicalgraph.zigbee.zcl.DataType
metadata {
	definition(name: "UIOT Motion Detector", namespace: "oukene", author: "oukene", runLocally: false, mnmn: "SmartThings", vid: "generic-motion-2") {
		capability "Motion Sensor"
		capability "Configuration"
		capability "Battery"
		capability "Refresh"
		//capability "Health Check"
		capability "Sensor"
		
		fingerprint profileId: "0104", deviceId: "0402", inClusters: "0000,0001,0003,0500", outClusters: "0003", manufacturer: "eWeLink", model: "MS01", deviceJoinName: "eWeLink Motion Sensor" //eWeLink Motion Sensor
		fingerprint profileId: "0104", deviceId: "0402", inClusters: "0000,0003,0500,0001", manufacturer: "ORVIBO", model: "895a2d80097f4ae2b2d40500d5e03dcc", deviceJoinName: "Orvibo Motion Sensor" //Orvibo Motion Sensor
		fingerprint profileId: "0104", deviceId: "0402", inClusters: "0000,0003,0500,0001,FFFF", manufacturer: "Megaman", model: "PS601/z1", deviceJoinName: "INGENIUM Motion Sensor" //INGENIUM ZB PIR Sensor
		fingerprint profileId: "0104", deviceId: "0402", inClusters: "0000, 0003, 0500, 0001", outClusters: "0019", manufacturer: "HEIMAN", model: "PIRSensor-N", deviceJoinName: "HEIMAN Motion Sensor" //HEIMAN Motion Sensor
		fingerprint profileId: "0104", deviceId: "0402", inClusters: "0000, 0001, 0500", outClusters: "0019", manufacturer: "Third Reality, Inc", model: "3RMS16BZ", deviceJoinName: "ThirdReality Motion Sensor" //ThirdReality Motion Sensor
        fingerprint profileId: "0104", deviceId: "0402", inClusters: "0000 0001 0003 0500", manufacturer: "Konke", model: "3AFE28010402000D", deviceJoinName: "UIOT-MM40S" //UIOT Motion Sensor
	}
    
	simulator {
		status "active": "zone status 0x0001 -- extended status 0x00"
		for (int i = 0; i <= 100; i += 11) {
			status "battery ${i}%": "read attr - raw: 2E6D01000108210020C8, dni: 2E6D, endpoint: 01, cluster: 0001, size: 08, attrId: 0021, encoding: 20, value: ${i}"
		}
	}
	tiles(scale: 2) {
		multiAttributeTile(name: "motion", type: "generic", width: 6, height: 4) {
			tileAttribute("device.motion", key: "PRIMARY_CONTROL") {
				attributeState "active", label: 'motion', icon: "st.motion.motion.active", backgroundColor: "#00A0DC"
				attributeState "inactive", label: 'no motion', icon: "st.motion.motion.inactive", backgroundColor: "#cccccc"
			}
		}
		valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
			state "battery", label: '${currentValue}% battery', unit: ""
		}
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", action: "refresh.refresh", icon: "st.secondary.refresh"
		}
		main(["motion"])
		details(["motion","battery", "refresh"])
	}
    
    preferences
    {
    	// The sensor sleeps for a minute after detecting motion and does not send an 'inactive' report.
        // The device handler will, by default, reset the motion state after sixty-five seconds to allow
        // a prompt new motion report to arrive first and restart the timer.
		input 'motionreset', 'number', title: 'Motion Reset Period', description: 'Enter number of seconds (default = 65)', range: '1..7200'
	}
}

def stopMotion() {
	log.debug "motion inactive"
	sendEvent(getMotionResult(false))
}

def installed(){
	log.debug "installed"
	return zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, 0x0021) +
					zigbee.readAttribute(zigbee.IAS_ZONE_CLUSTER,zigbee.ATTRIBUTE_IAS_ZONE_STATUS)

}

def parse(String description) {
	log.debug "description(): $description"
	def map = zigbee.getEvent(description)
	ZoneStatus zs
	if (!map) {
		if (description?.startsWith('zone status')) {
			zs = zigbee.parseZoneStatus(description)
			map = parseIasMessage(zs)
		} else {
			def descMap = zigbee.parseDescriptionAsMap(description)
			if (descMap?.clusterInt == zigbee.POWER_CONFIGURATION_CLUSTER) {
				map = batteyHandler(description)
			} else if (descMap?.clusterInt == zigbee.IAS_ZONE_CLUSTER && descMap.commandInt != 0x07 && descMap.value) {
				log.debug "parseDescriptionAsMap: $descMap.value"
				zs = new ZoneStatus(zigbee.convertToInt(descMap.value, 16))
				map = parseIasMessage(zs)
			}
		}
	}
	log.debug "Parse returned $map"
	def result = map ? createEvent(map) : [:]
	if (description?.startsWith('enroll request')) {
		List cmds = zigbee.enrollResponse()
		log.debug "enroll response: ${cmds}"
		result = cmds?.collect { new physicalgraph.device.HubAction(it) }
	}

	return result
}

def batteyHandler(String description){
	def descMap = zigbee.parseDescriptionAsMap(description)
	def map = [:]
	if (descMap?.clusterInt == zigbee.POWER_CONFIGURATION_CLUSTER && descMap.commandInt != 0x07 && descMap.value) {
		map = getBatteryPercentageResult(Integer.parseInt(descMap.value, 16))
	}
	return map
}

def parseIasMessage(ZoneStatus zs) {
	Boolean motionActive = zs.isAlarm1Set() || zs.isAlarm2Set()
	if (!supportsRestoreNotify()) {
		if (motionActive) {
			def timeout = motionreset ? motionreset : 65
			log.debug "Stopping motion in ${timeout} seconds"
			runIn(timeout, stopMotion)
		}
	}
	return getMotionResult(motionActive)
}

def supportsRestoreNotify() {
	return (getDataValue("manufacturer") == "eWeLink") || (getDataValue("manufacturer") == "Third Reality, Inc")
}

def getBatteryPercentageResult(rawValue) {
	log.debug "Battery Percentage rawValue = ${rawValue} -> ${rawValue / 2}%"
	def result = [:]
	def manufacturer = getDataValue("manufacturer")

	if (0 <= rawValue && rawValue <= 200) {
		result.name = 'battery'
		result.translatable = true
		if (manufacturer == "Third Reality, Inc") {
			result.value = Math.round(rawValue)
		} 
        else if(manufacturer == "Konke") {
        	def volts = rawValue / 10
            def minVolts = 2.1
            def maxVolts = 3.0
            def pct = (volts - minVolts) / (maxVolts - minVolts)
            def roundedPct = Math.round(pct * 100)
            if(roundedPct <=0)
            	roundedPct = 1
            result.value = Math.min(100, roundedPct)
        }
        else {
			result.value = Math.round(rawValue / 2)
		}
		result.descriptionText = "${device.displayName} battery was ${result.value}%"
	}
	return result
}

def getMotionResult(value) {
	def descriptionText = value ? "${device.displayName} detected motion" : "${device.displayName} motion has stopped"
	return [
			name			: 'motion',
			value			: value ? 'active' : 'inactive',
			descriptionText : descriptionText,
			translatable	: true
	]
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
	log.debug "ping "
    log.debug zigbee.readAttribute(zigbee.IAS_ZONE_CLUSTER, zigbee.ATTRIBUTE_IAS_ZONE_STATUS) + zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, 0x0021)
	return zigbee.readAttribute(zigbee.IAS_ZONE_CLUSTER, zigbee.ATTRIBUTE_IAS_ZONE_STATUS) + zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, 0x0021)
}

def refresh() {
	log.debug "Refreshing Values"
    log.debug zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, 0x0021)
	return  zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, 0x0021) +
					zigbee.readAttribute(zigbee.IAS_ZONE_CLUSTER,zigbee.ATTRIBUTE_IAS_ZONE_STATUS) +
					zigbee.enrollResponse()
}

def configure() {
	log.debug "configure"
	def manufacturer = getDataValue("manufacturer")
	if (manufacturer == "eWeLink") {
		sendEvent(name: "checkInterval", value:2 * 60 * 60 + 5 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
		return zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, 0x0021, DataType.UINT8, 30, 3600, 0x10) + refresh()
	} else if (manufacturer == "Third Reality, Inc") {
		return zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, 0x0021)
    } else if (manufacturer == "Konke") {
    	sendEvent(name: "checkInterval", value:7800, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
        return zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, 0x0021, DataType.UINT8, 30, 1200, 0x10) + refresh()
	} else {
		sendEvent(name: "checkInterval", value:20 * 60 + 2*60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
		return zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, 0x0021, DataType.UINT8, 30, 1200, 0x10) + refresh()

	}
}
