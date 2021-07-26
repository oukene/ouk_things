/**
 *  Copyright 2014 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
    // Automatically generated. Make future change here.
    definition (name: "RFID Food Trash Meter", namespace: "oukene", author: "oukene", mnmn: "oukene", vid: "3e85ff92-60fc-3685-83d8-c60c1a144bc1") {
    	capability "Refresh"
    	capability "orangeboard13471.trashmeter"
    	capability "orangeboard13471.updatetime"
    }
    
    preferences {
        input "tagId", "text", type: "text", title: "태그 ID", description: "카드에 적힌 TagID를 입력하세요", required: true
        input "aptDong", "text", title: "동", description: "아파트 동", required: true
        input "aptHo", "text", title: "호", description: "아파트 호", required: true
        input "under20Kg", "decimal", title: "20kg 이하 요금", defaultValue: 187, description: "20Kg 이하일 때 KG당 요금 기본값 : 187", required: false
        input "beteen20Kg", "decimal", title: "20kg ~30KG 요금", defaultValue: 280, description: "20Kg ~ 30KG 일 때 KG당 요금 기본값 : 280", required: false
        input "upper30Kg", "decimal", title: "30kg 이상 요금", defaultValue: 327, description: "30Kg 이상일 때 KG당 요금 기본값 : 327", required: false
        input "queryMonth", "text", title: "조회년월", description: "ex)202106 (비워둘경우 현재 월)", required: false
    }
}

// Parse incoming device messages to generate events
def parse(String description) {
	log.debug "Parsing '${description}'"
}

def installed() {
    initialize()
}

def updated() {
    initialize()
}

def uninstalled() {
    unschedule()
}


def initialize() {
	unschedule()
    
    if (settings.under20Kg == null || settings.under20Kg == "" ) settings.under20Kg = 187
    if (settings.beteen20Kg == null || settings.beteen20Kg == "" ) settings.beteen20Kg = 280
    if (settings.upper30Kg == null || settings.upper30Kg == "" ) settings.upper30Kg = 327
   
    refresh()
    //runEvery1Minutes()
    schedule("0 0 0/1 * * ?", pollTrash)
}

def refresh() {
    log.debug "refresh()"

    pollTrash()
}

def configure() {
    log.debug "Configuare()"
}

def setWeight(weight)
{
	log.debug "setWeight"
	sendEvent(name: "weight", value: weight, displayed: true)
}

def setCharge(charge)
{
	log.debug "setCharge"
	sendEvent(name: "charge", value: charge, displayed: true)
}

def setCount(count)
{
	log.debug "setCount"
	sendEvent(name: "count", value: count, displayed: true)
}


def setSummury(text)
{
	log.debug "setSummury"
	sendEvent(name: "summury", value: text, displayed: true)
}

def setUpdateTime(time)
{
	log.debug "setUpdateTime"
    sendEvent(name: "updatetime", value: time, displayed: true)
}


def pollTrash() {
    log.debug "pollTrash()"
    if (tagId && aptDong && aptHo) {

        def sdf = new java.text.SimpleDateFormat("yyyyMMdd")
        Date now
        if(queryMonth != null)
        {
			now = sdf.parse(queryMonth+"01");
        }
        else
       	{
            now = new Date()
		}
        
        Calendar calendar = Calendar.getInstance()
        calendar.setTime(now)

        // cal first day of month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH))
        def firstDateStr = sdf.format(calendar.getTime())

        calendar.add(Calendar.MONTH, 1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.add(Calendar.DATE, -1)

        def lastDateStr = sdf.format(calendar.getTime())

        log.debug "First day: " + firstDateStr
        log.debug "Last day: " + lastDateStr

        def pageIdx = 1

        def params = [
                "uri" : "https://www.citywaste.or.kr/portal/status/selectDischargerQuantityQuickMonthNew.do?tagprintcd=${tagId}&aptdong=${aptDong}&apthono=${aptHo}&startchdate=${firstDateStr}&endchdate=${lastDateStr}&pageIndex=${pageIdx}",
                "contentType" : 'application/json'
        ]

        int fare = 0
        def totalQty = 0
        def totalCount = 0
        try {
            log.debug "request >> ${params}"
            
            def respMap = getHttpGetJson(params)
            
            if(respMap != null){
                if(respMap.totalCnt != 0){
                	totalCount = respMap.totalCnt
                    def pages = respMap.paginationInfo.totalPageCount

                    log.debug "total pages >> ${pages}"
                    
                    for(def i = 1 ; i < pages + 1 ; i++){
                    	params = [
                                "uri" : "https://www.citywaste.or.kr/portal/status/selectDischargerQuantityQuickMonthNew.do?tagprintcd=${tagId}&aptdong=${aptDong}&apthono=${aptHo}&startchdate=${firstDateStr}&endchdate=${lastDateStr}&pageIndex=${i}",
                                "contentType" : 'application/json'
                        ]
                        respMap = getHttpGetJson(params)
                    
                        def lists = respMap.list

                        for(def j=0;j<lists.size();j++){
                            totalQty += lists[j].qtyvalue
                            log.debug lists[j].dttime + " : " + lists[j].qtyvalue
                        }
                    }
                }else{
                    log.debug "there is no data in this month"
                }
                fare = cal_fare(totalQty)

                log.debug "weight ${totalQty} fare ${fare}"
				fare = fare - fare % 10
                setWeight(totalQty)
                setCharge(fare)
                setCount(totalCount)
                summury = totalCount + "회 / " + totalQty + "kg / " + fare + "원"
                def time = new Date().format('yyyy-MM-dd HH:mm:ss', location.getTimeZone())
                setUpdateTime(time)
            }else{
                log.warn "retry to pollTrash cause server error try after 10 sec"
                runIn(10, pollTrash)
                //pollTrash()
            }
        } catch (e) {
            log.error "failed to update $e"
        }
    }
    else log.error "Missing settings tagId or aptDong or aptHo"
}

private getHttpGetJson(param) {
    log.debug "getHttpGetJson>> params : ${param}"
    def jsonMap = null
    try {
        httpGet(param) { resp ->
            log.debug "getHttpGetJson>> resp: ${resp.data}"
            jsonMap = resp.data
        }
    } catch(groovyx.net.http.HttpResponseException e) {
        log.error "getHttpGetJson>> HTTP Get Error : ${e}"
    }

    return jsonMap

}

private cal_fare(weight){
    log.debug "cal_fare weight is ${weight} fare late ~20Kg ${under20Kg} 20Kg~30Kg ${beteen20Kg} 30Kg~ ${upper30Kg}"

    def sum = 0
    if (weight < 20){
        sum = settings.under20Kg * weight
    }
    else{
        sum = settings.under20Kg * 20
    }

    if (weight > 20){
        if (weight < 30){
            sum += settings.beteen20Kg * (weight - 20)
        }
        else{
            sum += settings.beteen20Kg * 10
        }
    }
    if (weight > 30){
        sum += settings.upper30Kg * (weight - 30)
    }
    return sum
}