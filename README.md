# TiniBot2.0


## Installation

Update the `discord-bot-token` to the application.conf file. 
You obtain it by adding an App via [Discord Developer Portal](https://discordapp.com/developers/applications/me).

* Choose "New App"
* Create and select a new App
* Click "Create Bot User". 

Also, update the `kill-secret` or anyone can shutdown your bot.



## Commands

* `!ping`
* `!kill <secret>`



## Infrastrtucture

I guess it would be interesting to use some nice and new technologies.

I'd vote for a kafka cluster, a cassandra cluster being the data backbone of all this and operating with streams on top of them.

Or of course, we could simply do akka persistence (but not with the typed actors)