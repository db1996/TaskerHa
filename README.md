# <img src="docs/logo.png" alt="Logo" width="50"/>TaskerHA

A Tasker plugin to fully integrate HomeAssistant into your workflow!

You can respond to entity state changes, call any HA service, or retrieve an entity's state.

TaskerHA lets you:

- Call any Home Assistant service from a Tasker action
- Read the state and attributes of any entity
- Trigger Tasker profiles when an entity changes state over a websocket connection


Table of contents:
- [TaskerHA](#taskerha)
  - [Requirements](#requirements)
  - [Download](#download)
  - [Quick start](#quick-start)
  - [Usage](#usage)
    - [Setup](#setup)
    - [Call service action](#call-service-action)
      - [Response in tasker](#response-in-tasker)
      - [Error codes](#error-codes)
    - [Get State action](#get-state-action)
      - [Response in tasker](#response-in-tasker-1)
      - [Error codes](#error-codes-1)
    - [Trigger state change profile](#trigger-state-change-profile)
      - [Response in tasker](#response-in-tasker-2)
      - [Error codes](#error-codes-2)


## Requirements

- <a href="https://tasker.joaoapps.com/" target="_blank">Tasker</a>
- <a href="https://www.home-assistant.io/" target="_blank">Home assistant</a>

## Download

<a  href="https://github.com/db1996/TaskerHa/releases/latest">
<img width="150" height="auto" src="https://user-images.githubusercontent.com/69304392/148696068-0cfea65d-b18f-4685-82b5-329a330b1c0d.png"
alt="Download from GitHub releases" /></a>
<!-- <a  href="https://f-droid.org/packages/com.github.db1996.taskerha/"> -->
<!-- <img width="150" height="auto" src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" -->
<!-- alt="Download from fdroid" /></a> -->


![GitHub All Releases](https://img.shields.io/github/downloads/db1996/TaskerHa/total)
<!-- ![F-Droid Version](https://img.shields.io/f-droid/v/com.github.db1996.taskerha) -->

F-droid release coming soon

<!-- NOTE: You can not install the f-droid release next to the github release. You will have to uninstall and reinstall the version you want. Tasks/profiles should keep working but you'll have to configure your server again. -->



## Quick start

1. Create a <a href="https://www.home-assistant.io/docs/authentication/" target="_blank">Create a Long-lived access token</a> in your Home Assistant user profile.
2. [Download](#download) and install the APK.
3. Open the TaskerHA app and configure your Home Assistant URL and token.
4. In Tasker, add an "HA Call service" or "HA Get state" action.
5. (Optional) Enable websockets in the app and create an "HA On trigger state" profile to react to entity changes.

## Usage

### Setup

1. <a href="https://www.home-assistant.io/docs/authentication/" target="_blank">Create a Long-lived access token</a> in your Homeassistant. And save it for later in the process.
2. Install the app. For now, the APK is available <a href="https://github.com/db1996/TaskerHa/releases/latest" target="_blank">here in releases</a>, but in the future it will be available on f-droid (hopefully)
3. Open the app (outside of tasker) and fill in your Homeassistant server details. This works both over localhost and remote
   - Use the scheme and optionally the port in the url. Example: `http://192.168.1.xxx:8123` for a local instance. Or `https://ha.yourdomain.com`. For proxy servers you do **not** need to use a port.
   - Do not use a trailing backslash in the host.
   - NOTE: Turning on the websocket will prompt you to turn off battery optimization. Without it the websocket connection might be killed by android and profiles will no longer fire.
   - Turning off websockets in TaskerHa also means any profiles active will never fire.
   - All tasks will use the same server. Maybe a future enhancement will be multi server support but I am not sure how useful that would be.
4. Test the server. If it's correct, save!

### Call service action

1. Create a new task in tasker
2. Add action -> plugin -> taskerHa -> HA Call service
3. In the configuration of the task, there are searching and filtering options for all available services in your Homeassistant.
   1. Pick a service to call (filter through domains to make this easier to find)
   2. Once a service is picked. Any fields that need to be filled in will appear.
   3. If an entity_id is needed, an entity picker with searching options will apear. It will already be filtered by the domain of the picked service
   4. Any optional extra data (think light transition for example), can be turned off/on with the checkboxes. If it's turned off the value is not pushed to Homeassistant at all.

This should work for every single service in your Homeassistant, if there are issues with specific services you come accross, please create an issue!

Extra data where you usually see dropdowns in the Homeassistant UI will also have dropdowns here.

You can use tasker variables in any of the text fields and they will be replaced automatically. This includes all optional data.

**Example**

Turn on a light from Tasker:

- Action: Plugin -> TaskerHA -> HA Call service
- Service: `light.turn_on`
- Entity: `light.living_room`
- Optional data: set `brightness: 200`

#### Response in tasker

The following variables are available from within tasker after the action

| Variable | Function                                                                                                                                                                                                   |
| -------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| %ha_data | Complete API response (JSON), this will **usually** contain the new updated state of a called entity. But it's inconsistent from HA                                                                        |
| %err     | Error code, is 0 if no error occured. Check below for a complete list of error codes. If an error occurs it will also error the task itself unless you have "continue after error" turned on on the action |
| %errmsg  | Error message. Usually contains a friendly error message, with some java exception next to it.                                                                                                             |

#### Error codes

| Error code | Description                                                                                                                                                                        |
| ---------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1          | Can't connect to Home Assistant. This means the app could not ping HA. The error will contain more details depending on the reason. But usually this means it can't reach the host |
| 2          | Service call failed. This means that it can connect correctly with Homeassistant but the service call itself failed. %errmsg will contain more details                             |
| 3          | Unknown error occured. %errmsg will contain more details (java error)                                                                                                              |
| 4          | Invalid JSON Data, this means the app failed to map your data to a valid JSON format. This is about the optional options for service calls                                         |


### Get State action

1. Create a new task in tasker
2. Add action -> plugin -> taskerHa -> HA Get State
3. In the task configuration, there's an entity picker to search with. And you can filter by domain (fuzzy search)

You can use tasker variables for the entity ID, make sure to use the "%" for any variable use.

**Example**

Check if the alarm is armed:

- Action: Plugin -> TaskerHA -> HA Get state
- Entity: `alarm_control_panel.home_alarm`
- After the action, read `%ha_state` in Tasker.  
  For example, run different actions based on `%ha_state ~ armed_away` or `disarmed`.


#### Response in tasker

The following variables are available from within tasker after the action

| Variable  | Function                                                                                                                                                                                                   |
| --------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| %ha_state | Contains the current state of the choosen entity                                                                                                                                                           |
| %ha_attrs | Contains in JSON any attributes on the entity. For example, for lights you will get the color, brightness etc.                                                                                             |
| %ha_raw   | Raw JSON of the full API response from homeassistant.                                                                                                                                                      |
| %err      | Error code, is 0 if no error occured. Check below for a complete list of error codes. If an error occurs it will also error the task itself unless you have "continue after error" turned on on the action |
| %errmsg   | Error message. Usually contains a friendly error message, with some java exception next to it.                                                                                                             |

#### Error codes

| Error code | Description                                                                                                                                                                        |
| ---------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1          | Can't connect to Home Assistant. This means the app could not ping HA. The error will contain more details depending on the reason. But usually this means it can't reach the host |
| 2          | Get entity state call failed. This means that it can connect correctly with Homeassistant but the entity state call itself failed. %errmsg will contain more details               |
| 3          | Unknown error occured. %errmsg will contain more details (java error)                                                                                                              |


### Trigger state change profile

1. Create a new profile in Tasker: plugin -> taskerHa -> HA On trigger state
2. In the configuration, there's an entity picker to search with. And you can filter by domain (fuzzy search).
3. You can enter from and to fields to filter events based on the `old_state` and `new_state`. This works the same as a trigger in a Homeassistant automation (`for` is not possible right now)

You can use tasker variables for the entity ID, from and to states, make sure to use the "%" for any variable use.

The websocket option in the main app has to be turned on for this. Oterwise it will never fire.

Any state change to the choosen element will fire the profile

**Example**

Run a Tasker task when the door opens:

- Profile: Plugin -> TaskerHA -> HA On trigger state
- Entity: `binary_sensor.front_door`
- From: `off`
- To: `on`

Now any time the door opens, this profile will fire and you can run any Tasker task.

#### Response in tasker

The following variables are available from within tasker after the action

| Variable   | Function                                                                                                                                                                                                   |
| ---------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| %ha_to     | Contains the current state of the choosen entity. Example `on`                                                                                                                                             |
| %ha_from   | Contains the old state of the choosen entity. Example `off`                                                                                                                                                |
| %ha_attrs  | Contains in JSON any attributes on the entity for the new state. For example, for lights you will get the color, brightness etc.                                                                           |
| %ha_entity | Contains the entity_id of the choosen entity                                                                                                                                                               |
| %ha_raw    | Raw JSON of the full entity state change response from homeassistant.                                                                                                                                      |
| %err       | Error code, is 0 if no error occured. Check below for a complete list of error codes. If an error occurs it will also error the task itself unless you have "continue after error" turned on on the action |
| %errmsg    | Error message. Usually contains a friendly error message, with some java exception next to it.                                                                                                             |

#### Error codes

| Error code | Description                                                           |
| ---------- | --------------------------------------------------------------------- |
| 3          | Unknown error occured. %errmsg will contain more details (java error) |