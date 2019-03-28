#!/usr/bin/env groovy

import groovyx.net.http.ContentType
import groovyx.net.http.RESTClient

@Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7.1')

CliBuilder cli = new CliBuilder(usage: "./${this.class.getName()}.groovy <options>")
cli.with {
    s longOpt: 'slackToken', args: 1, required: true, argName: 'token', 'Slack User Token'
    a longOpt: 'darkSkyApiKey', args: 1, required: true, argName: 'api-key', 'Dark Sky API Key'
    l longOpt: 'latitude', args: 1, required: false, argName: 'latitude', 'Latitude (format: (-?)00.0000)'
    n longOpt: 'longitude', args: 1, required: false, argName: 'longitude', 'Longitude (format: (-?)000.0000)'
    u longOpt: 'units', args: 1, required: false, argName: 'units', 'Default units of measurement (options: metric, imperial)'
}

OptionAccessor options = cli.parse(args)
if (options == null) System.exit(1)

String slackUserToken = options.slackToken
String darkSkyApiKey = options.darkSkyApiKey
String latitude = options.latitude ? options.latitude : "47.61002"
String longitude = options.longitude ? options.longitude : "-122.18785"
UnitsOfMeasurement defaultUnits
try {
   defaultUnits = UnitsOfMeasurement.valueOf(options?.units.toUpperCase())
}
catch (Exception e) {
   defaultUnits = UnitsOfMeasurement.METRIC
}

Map condition = getCondition(darkSkyApiKey, latitude, longitude)

int tempF = Math.round(condition.temperature)
int tempC = Math.round(((condition.temperature - 32) / 1.8)) 

String conditionText = condition?.summary
String tempText = defaultUnits == UnitsOfMeasurement.METRIC ?
      "$tempC°C ($tempF°F)" : "$tempF°F ($tempC°C)"

String statusText = (conditionText) ?
      "Weather: $conditionText $tempText" : 'Look out the window :)'
String emoji = ":" +mapIconToSlackIcon(new String(condition?.icon?:"unknown"))  + ":"

def slackRS = setSlackStatus(statusText, emoji, slackUserToken)
println slackRS.statusLine

def getCondition(String darkSkyApiKey, String latitude, String longitude) {
    def conditionRs = new RESTClient('https://api.darksky.net/').get(
        path: '/forecast/' + darkSkyApiKey + '/' + latitude + "," + longitude
    )

    return conditionRs.data.currently
}

def setSlackStatus(String statusText, String emoji, String token) {
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

def mapIconToSlackIcon(String darkSkyIcon) {
    def icons = [ 
        'clear-day': 'wx-clear',
        'clear-night': 'wx-clear',
        'rain': 'umbrella_with_rain_drops',
        'snow': 'wx-snow',
        'sleet': 'wx-sleet',
        'wind': 'wind_blowing_face',
        'fog': 'wx-fog',
        'cloudy': 'wx-cloudy',
        'partly-cloudy-day': 'wx-partlycloudy',
        'partly-cloudy-night': 'wx-partlycloudy',
        'unknown': 'wx-unknown'
    ]

    return icons[darkSkyIcon] ?: 'wx-unknown'
}

enum UnitsOfMeasurement {
   METRIC, IMPERIAL
}
