#!/usr/bin/env groovy

import groovyx.net.http.ContentType
import groovyx.net.http.RESTClient

@Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7.1')

CliBuilder cli = new CliBuilder(usage: "./${this.class.getName()}.groovy <options>")
cli.with {
    s longOpt: 'slackToken', args: 1, required: true, argName: 'token', 'Slack User Token'
    a longOpt: 'owmApiKey', args: 1, required: true, argName: 'api-key', 'OpenWeatherMap API Key'
    l longOpt: 'latitude', args: 1, required: false, argName: 'latitude', 'Latitude (format: (-?)00.0000)'
    n longOpt: 'longitude', args: 1, required: false, argName: 'longitude', 'Longitude (format: (-?)000.0000)'
    u longOpt: 'units', args: 1, required: false, argName: 'units', 'Default units of measurement (options: metric, imperial)'
}

OptionAccessor options = cli.parse(args)
if (options == null) System.exit(1)

String slackUserToken = options.slackToken
String owmApiKey = options.owmApiKey
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

int tempF = Math.round(condition.main.temp)
int tempC = Math.round(((tempF - 32) / 1.8)) 

String conditionText = condition?.weather[0].description.capitalize()
String tempText = defaultUnits == UnitsOfMeasurement.METRIC ?
      "$tempC°C ($tempF°F)" : "$tempF°F ($tempC°C)"

String statusText = (conditionText) ?
      "Weather: $conditionText $tempText" : 'Look out the window :)'
String emoji = ":${mapIconToSlackIcon(new String(condition.weather[0].main))}:"

def slackRS = setSlackStatus(statusText, emoji, slackUserToken)
println slackRS.statusLine

def getCondition(String darkSkyApiKey, String latitude, String longitude) {
    def conditionRs = new RESTClient('https://api.openweathermap.org/').get(
        path: "/data/2.5/weather/?lat=$latitude&lon=$longitude&units=imperial&appId=$owmApiKey"
    )

    return conditionRs
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

// https://openweathermap.org/weather-conditions
def mapIconToSlackIcon(String darkSkyIcon) {
    def icons = [ 
        'Thunderstorm': 'wx-tstorms',
        'Drizzle': 'umbrella_with_rain_drops',
        'Rain': 'umbrella_with_rain_drops',
        'Snow': 'wx-snow',
        'Mist': 'wx-fog',
        'Haze': 'wx-hazy',
        'Smoke': 'wx-hazy',
        'Dust': 'wx-hazy',
        'Fog': 'wx-fog',
        'Sand': 'wx-hazy',
        'Ash': 'wx-hazy',
        'Squall': 'wx-rain',
        'Tornado': 'wind_blowing_face',
        'Clear': 'wx-clear',
        'Clouds': 'wx-mostlycloudy'
    ]

    return icons[darkSkyIcon] ?: 'wx-unknown'
}

enum UnitsOfMeasurement {
   METRIC, IMPERIAL
}

//String slackUserToken = 'xoxp-9449251159-224481289394-258134644199-0429842fd8b6a8c63eab467091a8f245'
