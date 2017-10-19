#!/usr/bin/env groovy

import groovyx.net.http.ContentType
import groovyx.net.http.RESTClient

@Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7.1')

CliBuilder cli = new CliBuilder(usage: "./${this.class.getName()}.groovy <options>")
cli.with {
    s longOpt: 'slackToken', args: 1, required: true, argName: 'token', 'Slack User Token'
    w longOpt: 'wundergroundApiKey', args: 1, required: true, argName: 'api-key', 'Weather Undergound API Key'
    l longOpt: 'location', args: 1, required: false, argName: 'location', 'Location (format: City, State)'
    u longOpt: 'units', args: 1, required: false, argName: 'units', 'Default units of measurement (options: metric, imperial)'
}

OptionAccessor options = cli.parse(args)
if (options == null) System.exit(1)

String slackUserToken = options.slackToken
String wundergroundApiKey = options.wundergroundApiKey
String location = options.location ? options.location : "Bellevue, WA"
UnitsOfMeasurement defaultUnits
try {
   defaultUnits = UnitsOfMeasurement.valueOf(options?.units.toUpperCase())
}
catch (Exception e) {
   defaultUnits = UnitsOfMeasurement.METRIC
}

String formattedLocation = parseLocation(location)
Map condition = getCondition(wundergroundApiKey, formattedLocation)
String tempC = condition?.temp_c
String tempF = condition?.temp_f
String conditionText = condition?.weather
String tempText = defaultUnits == UnitsOfMeasurement.METRIC ?
      "$tempC°C ($tempF°F)" : "$tempF°F ($tempC°C)"

String statusText = (conditionText) ?
      "Weather: $conditionText $tempText" : 'Look out the window :)'
String emoji = ":wx-${condition?.icon}:"

def slackRS = setSlackStatus(statusText, emoji, slackUserToken)
println slackRS.statusLine

static def getCondition(String wundergroundApiKey, String locationFormatted) {
    def wundergroundRS = new RESTClient('http://api.wunderground.com').get(
        path: "/api/$wundergroundApiKey/conditions/q/${locationFormatted}.json"
    )

    return wundergroundRS?.data?.current_observation
}

static def parseLocation(String location) {
   String[] locationPieces = location.split(",")
   if (locationPieces.length != 2) {
      throw new IllegalArgumentException("location passed (" + location +
         ") was not of format 'Detroit, MI'")
   }

   return locationPieces[1].trim() + "/" + locationPieces[0].trim()
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

enum UnitsOfMeasurement {
   METRIC, IMPERIAL
}
