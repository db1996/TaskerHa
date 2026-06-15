# <img src="docs/logo.png" alt="Logo" width="50"/>TaskerHA

A Tasker plugin to fully integrate HomeAssistant into your workflow!

You can respond to entity state changes, call any HA service, or retrieve an entity's state.

TaskerHA lets you:

- Call any Home Assistant service from a Tasker action
- Read the state and attributes of any entity
- Trigger Tasker profiles when an entity (or a specific attribute of an entity) changes state over a websocket connection
- Send direct messages from HA to tasker and back with a custom event (uses websocket)

**[Full documentation](https://taskerha.db1996-gh.com/)**

## Requirements

- <a href="https://tasker.joaoapps.com/" target="_blank">Tasker</a>
- <a href="https://www.home-assistant.io/" target="_blank">Home assistant</a>

## Download

<a  href="https://github.com/db1996/TaskerHa/releases/latest">
<img width="150" height="auto" src="https://user-images.githubusercontent.com/69304392/148696068-0cfea65d-b18f-4685-82b5-329a330b1c0d.png"
alt="Download from GitHub releases" /></a>
<a  href="https://f-droid.org/packages/com.github.db1996.taskerha/">
<img width="150" height="auto" src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
<!-- alt="Download from fdroid" /></a>


![GitHub All Releases](https://img.shields.io/github/downloads/db1996/TaskerHa/total)

<!-- NOTE: You can not install the f-droid release next to the github release. You will have to uninstall and reinstall the version you want. Tasks/profiles should keep working but you'll have to configure your server again. -->



## Quick start

1. Create a <a href="https://www.home-assistant.io/docs/authentication/" target="_blank">Create a Long-lived access token</a> in your Home Assistant user profile.
2. [Download](#download) and install the APK.
3. Open the TaskerHA app and configure your Home Assistant URL and token.
4. In Tasker, add an "HA Call service" or "HA Get state" action.
5. (Optional) Enable websockets in the app and create an "HA On trigger state" profile to react to entity changes.

## Support

<a href="https://www.buymeacoffee.com/db1996" target="_blank"><img src="https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png" alt="Buy Me A Coffee" style="height: 41px !important;width: 174px !important;box-shadow: 0px 3px 2px 0px rgba(190, 190, 190, 0.5) !important;-webkit-box-shadow: 0px 3px 2px 0px rgba(190, 190, 190, 0.5) !important;" ></a>
