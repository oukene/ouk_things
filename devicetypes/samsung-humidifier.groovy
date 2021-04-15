/*
 * BlueSky Humidified air purifier" Handler
 *
 *
 * 2021.01.19 Initial version
 *
 */

metadata {
    definition(name: "samsung_humidifier", namespace: "oukene", author: "oukene") {
        capability "Refresh"
    }
    
    command "Humidi_On"
    command "Humidi_Off"
    command "Light_On"
    command "Light_Off"
    command "Sterilize_On"
    command "Sterilize_Off"
    command "SterilizeClean_On"
    command "SterilizeClean_Off"
    command "Volume_Mute"
    command "Volume_UnMute"

    preferences {
    input name: "deviceid", type: "text", title: "Device ID", required: false
    input name: "token", type: "text", title: "Token", required: false
    input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
    }
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    if (logEnable) runIn(1800, logsOff)
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def parse(String description) {
    if (logEnable) log.debug(description)
}

def buildParms(String capability_, String command_, arg_ = ""){
    def builder = new groovy.json.JsonBuilder()
    def c
    
    if (arg_ instanceof List){
        c = [component:"main", capability:capability_, command:command_, arguments:arg_.collect()]
    }
    else if(arg_ != ""){
        def d = [arg_]
        c = [component:"main", capability:capability_, command:command_, arguments:d.collect()]
    }
    else{
        c = [component:"main", capability:capability_, command:command_]
    }
    
    builder commands:[c]

    def params = [
        uri: "https://api.smartthings.com/v1/devices/" + deviceid + "/commands",
        headers: ['Authorization' : "Bearer " + token],
        body: builder.toString()
    ]
    log.debug(builder.toString())
    return params
}


def post(params){
    try {
        httpPostJson(params) { resp ->
            //if (resp.success) {
            //}
            if (logEnable)
                if (resp.data) log.debug "${resp.data}"
        }
    } catch (Exception e) {
        log.warn "Call to on failed: ${e.message}"
    }
}

def Humidi_On(){
    def params = buildParms("execute", "execute", ["mode/vs/0",["x.com.samsung.da.options":["Humidi_On"]]])
    post(params)
}

def Humidi_Off(){
    def params = buildParms("execute", "execute", ["mode/vs/0",["x.com.samsung.da.options":["Humidi_Off"]]])
    post(params)
}

def Light_On(){
    def params = buildParms("execute", "execute", ["mode/vs/0",["x.com.samsung.da.options":["Light_On"]]])
    post(params)
}

def Light_Off(){
    def params = buildParms("execute", "execute", ["mode/vs/0",["x.com.samsung.da.options":["Light_Off"]]])
    post(params)
}

def Sterilize_On(){
    def params = buildParms("execute", "execute", ["mode/vs/0",["x.com.samsung.da.options":["Sterilize_On"]]])
    post(params)
}

def Sterilize_Off(){
    def params = buildParms("execute", "execute", ["mode/vs/0",["x.com.samsung.da.options":["Sterilize_Off"]]])
    post(params)
}

def SterilizeClean_On(){
    def params = buildParms("execute", "execute", ["mode/vs/0",["x.com.samsung.da.options":["SterilizeClean_On"]]])
    post(params)
}

def SterilizeClean_Off(){
    def params = buildParms("execute", "execute", ["mode/vs/0",["x.com.samsung.da.options":["SterilizeClean_Off"]]])
    post(params)
}

def Volume_Mute(){
    def params = buildParms("execute", "execute", ["mode/vs/0",["x.com.samsung.da.options":["Volume_Mute"]]])
    post(params)
}

def Volume_UnMute(){
    def params = buildParms("execute", "execute", ["mode/vs/0",["x.com.samsung.da.options":["Volume_100"]]])
    post(params)
}

