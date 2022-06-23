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
    definition (name: "Naver Shopping", namespace: "oukene", author: "oukene", mnmn: "oukene", vid: "df157cd4-579c-357a-a71e-1b856769aaec") {
    	capability "orangeboard13471.navershopping"
    	capability "orangeboard13471.updatetime"
        capability "Refresh"
    }
    
    preferences {
        input "clientID", "text", type: "text", title: "Client ID", description: "Input Client ID", required: true
        input "clientSecret", "text", title: "Client Secret", description: "Input Client Secret", required: true
        input("sortType", "enum", title: "검색타입", defaultValue: "sim", description: "검색타입", options: [
                "sim":"유사도순",
                "asc":"낮은가격순",
                "dsc": "높은가격순",
                "date": "등록일자순"])
        input "refreshInterval", "decimal", title: "조회 주기", defaultValue: 60, description: "조회 주기(분)", required: true
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
    
    refresh()
    //runEvery1Minutes()
    def minutes = Math.max(settings.refreshInterval.toInteger(),1)
    def cron = "0 0/${minutes} * * * ?"
    schedule(cron, pollPrice)
    //schedule("0 0 0/1 * * ?", pollTrash)
}

def refresh() {
    log.debug "refresh()"

    pollPrice()
}

def configure() {
    log.debug "Configuare()"
}

def setSummury(text)
{
	log.debug "setSummury"
	sendEvent(name: "summury", value: text, displayed: true)
}


def setPrice(text)
{
	log.debug "setPrice"
	sendEvent(name: "price", value: text, displayed: true)
}

def setUpdateTime(time)
{
	log.debug "setUpdateTime"
    sendEvent(name: "updatetime", value: time, displayed: true)
}


def pollPrice() {
    log.debug "pollTrash()"
    if (clientID && clientSecret) {
        def params = [
                "uri" : "https://openapi.naver.com/v1/search/shop.json?query=${device.name}&display=1&start=1&sort=${sortType}",
                "contentType" : 'application/json',
                "headers": [
                	"X-Naver-Client-Id": clientID,
                    "X-Naver-Client-Secret": clientSecret
                ]
        ]
        
        int fare = 0
        def totalQty = 0
        def totalCount = 0
        try {
            log.debug "request >> ${params}"
            def respMap = getHttpGetJson(params)
            
            if(respMap != null){
            	setSummury(respMap.items[0].lprice + "원")
            	setPrice(respMap.items[0].lprice)
                def time = new Date().format('yyyy-MM-dd HH:mm:ss', location.getTimeZone())
                setUpdateTime(time)
                //summury = "error, try later..."
            }else{
                log.warn "retry to pollPrice cause server error try after 10 sec"
                //summury = "error, try later..."
                runIn(10, pollPrice)
                //pollTrash()
            }
        } catch (e) {
            log.error "failed to update $e"
            summury = "error, try later..."
        }
    }
    else log.error "Missing settings clientID or clientSecret"
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