#!/usr/bin/env groovy

import groovyx.net.http.ContentType
import groovyx.net.http.RESTClient

@Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7.1')

String slackUserToken = ''
String wundergroundApiKey = ''

Map condition = getCondition(wundergroundApiKey)

String tempC = condition?.temp_c
String tempF = condition?.temp_f
String conditionText = condition?.weather

String statusText = (conditionText) ? "Weather: $conditionText $tempC°C ($tempF°F)" : 'Look out the window :)'
String emoji = ":wx-${condition?.icon}:"

def slackRS = setSlackStatus(statusText, emoji, slackUserToken)
println slackRS.statusLine

static def getCondition(String wundergroundApiKey) {
    def wundergroundRS = new RESTClient('http://api.wunderground.com').get(
        path: "/api/$wundergroundApiKey/conditions/q/WA/Bellevue.json"
    )

    return wundergroundRS?.data?.current_observation
}

static def setSlackStatus(String statusText, String emoji, String token) {
    println "Updating status: status_text=\"$statusText\", status_emoji=\"$emoji\""

    return new RESTClient('https://slack.com').post(
        path: '/api/users.profile.set',
        requestContentType: ContentType.URLENC,
        query: [
            token: token
        ],
        body: [
            profile: new groovy.json.JsonBuilder(
                status_text: statusText,
                status_emoji: emoji
            ).toString()
        ]
    )
}
