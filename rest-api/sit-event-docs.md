# Sit-Events Documentation

The SIT-Events system can be used to receive change notifications of data without needing to poll the information from
the api endpoints. This enables use cases like:

* refreshing a user interface in case information changes
* doing something in a backend in case information changes
* updating caches in case information changes

The events backend provides change notifications for the following data:

1. Journey Details (called in case one or more events along a journey route change)
2. Journey Positions (called in case position related data of a journey changes [such as speed])
3. Server Details (called in case detail information about a server change)
4. Dispatch Post Details (called in case detail information about a dispatch post change)

### Keep-Alive

The backend server sends a WebSocket ping frame in a regular interval, to which the client must respond with a WebSocket
pong frame within 30 seconds. If the client fails to do so, the server forcibly closes the connection. The client can
check if the connection is still alive in two ways:

1. send a WebSocket ping frame to the server, to which the server responds with a WebSocket pong frame
2. send a WebSocket text frame with the content `ping`, to which the server responds with a WebSocket text message with
   the content `pong`.

### Connecting and (un-) subscribing

The communication for SIT-Events is done via WebSocket. This is done by sending an http request with upgrade intent to
`wss://apis.simrail.tools/sit-events`. To subscribe or unsubscribe from updates, a text message in the following format
must be sent to the server: `sit-events/<action>/<data-type>/<version>/<server-id>/<data-id>`. Multiple messages can be
sent to subscribe to multiple data updates types on the same connection. The placeholders must be filled with the
following replacements:

| Placeholder   | Required Value                                                                                                                                                                                                                                                                               |
|---------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `<action>`    | The requested action to take, can be `subscribe` or `unsubscribe`                                                                                                                                                                                                                            |
| `<data-type>` | The type of data being requested, can be one of: `servers` (for server updates), `dispatch-posts` (for dispatch post updates), `journey-details` (for updates when one or more events along a journey route are updated), `journey-positions` (for updates on position related journey data) |
| `<version>`   | The requested data version (defines the way how update frames sent to the client are encoded). Currently only version `v1` is in use for all data types.                                                                                                                                     |
| `<server-id>` | The id of the server with which the requested data is associated, e.g. the id of the server where a journey is active.                                                                                                                                                                       |
| `<data-id>`   | The specific id of the data to receive updates of, e.g. the id of a single journey to only receive updates of that journey. Can be `+` to subscribe to all updates of the data on the specified server.                                                                                      |

Example text messages to subscribe to data:

* `sit-events/subscribe/journey-details/v1/9db9b77d-a5ff-5385-89c3-6c6224e0824f/53fe9829-6ab1-5ac6-a81e-9bce07f8ecd0`:
  Subscribes to event updates of journey `53fe9829-6ab1-5ac6-a81e-9bce07f8ecd0` on server de1.
* `sit-events/subscribe/journey-details/v1/9db9b77d-a5ff-5385-89c3-6c6224e0824f/+`: Subscribes to all event updates of
  all journeys on de1.
* `sit-events/unsubscribe/journey-details/v1/9db9b77d-a5ff-5385-89c3-6c6224e0824f/+`: Removes the subscription to all
  event updates of all journeys on de1.
* `sit-events/unsubscribe/journey-positions/v1/9db9b77d-a5ff-5385-89c3-6c6224e0824f/+`: Subscribes to all position
  updates of all journeys on de1.

> [!WARNING]  
> If a server receives an invalid text message (neither a keep-alive ping text message nor a valid subscription string)
> the server will close the connection with the status code 1003 (not acceptable).

### Frames being sent by the server

Initially, all new subscriptions will cause an initial snapshot being sent to the client that subscribed. This is not
the case for Journey Details, as the update frames don't contain relevant information that is required to send out
initially. Update frames only contain the information that was changed, every other field is not included in the frame
data by the backend.

#### 0. Update Frame Wrapper

Each update frame sent by the server contains the following base fields:

* `frameType`: holding the type of data that was updated, can be `SERVER`, `DISPATCH_POST`, `JOURNEY_DETAILS`,
  `JOURNEY_POSITION`
* `updateType`: the type of update that the client is being notified about with the frame, can be `ADD`, `REMOVE`,
  `UPDATE`
* `frameData`: the data of the frame, values depend on the frame type and the update type. See detailed documentation
  about each frame below.

#### 1. Journey Detail Data

The update frame for journey details only contains the id of the journey that was updated. Re-Fetching the journey
detail must be done by the client using the SIT-Journeys api endpoint:

```json5
{
  "frameType": "JOURNEY_DETAILS",
  "updateType": "UPDATE",
  "frameData": {
    // the id of the updated journey (string, not null)
    "journeyId": "b8cbcf2d-abe4-5b51-9f24-6e7d181b7487"
  }
}
```

#### 2. Server Data

Add frames hold the following data:

```json5
{
  "frameType": "SERVER",
  "updateType": "ADD",
  "frameData": {
    // the id of the server (string, not null)
    "serverId": "9db9b77d-a5ff-5385-89c3-6c6224e0824f",
    // the code of the server (string, not null)
    "code": "de1",
    // tags of the server (array of string, not null)
    "tags": [
      "KEINE EVENTS"
    ],
    // the language that is spoken on the server (string, null for international servers)
    "spokenLanguage": "Deutsch",
    // the region where the server is located (enum, not null, `ASIA`, `EUROPE`, `US_NORTH`)
    "region": "EUROPE",
    // if the server is online (boolean, not null)
    "online": true,
    // the id of the timezone where the server is located (string, not null)
    "timezoneId": "+02:00",
    // the offset of the server time to utc, in hours (integer, not null)
    "utcOffsetHours": 2
  }
}
```

Update and delete frames hold the following data:

```json5
{
  "frameType": "SERVER",
  "updateType": "UPDATE",
  "frameData": {
    // the id of the server (string, not null)
    "serverId": "9db9b77d-a5ff-5385-89c3-6c6224e0824f",
    // if the server is online (boolean, not present if unchanged)
    "online": true,
    // the id of the timezone where the server is located (string, not present if unchanged)
    "timezoneId": "+02:00",
  }
}
```

#### 3. Dispatch Post Data

Add frames hold the following data:

```json5
{
  "frameType": "DISPATCH_POST",
  "updateType": "ADD",
  "frameData": {
    // the id of the dispatch post (string, not null)
    "postId": "20d7603f-746f-548e-9daa-52bd8138a332",
    // the id of the point associated with the dispatch post (string, not null)
    "pointId": "a4cb9d4f-d3ae-4e11-b627-503524fc6a4e",
    // the id of the server where the dispatch post is located (string, not null)
    "serverId": "9db9b77d-a5ff-5385-89c3-6c6224e0824f",
    // the name of the dispatch post (string, not null)
    "name": "Starzyny",
    // the difficulty level of the dispatch post (integer, 1-5, not null)
    "difficultyLevel": 4,
    // the latitude where the dispatch post is located (double, not null)
    "latitude": 50.71260070800781,
    // the longitude where the dispatch post is located (double, not null)
    "longitude": 19.801761627197266,
    // the steam ids of the dispatchers in the dispatch post (array of string, not null)
    "dispatcherSteamIds": [
      "76561198342066455"
    ]
  }
}
```

Update and delete frames hold the following data:

```json5
{
  "frameType": "DISPATCH_POST",
  "updateType": "UPDATE",
  "frameData": {
    // id of the dispatch post that was updated (string, not null)
    "postId": "edca696b-163f-5dd2-8cf2-5eb52b414977",
    // the steam ids of the dispatchers in the dispatch post (array of string, not present if unchanged)
    "dispatcherSteamIds": []
  }
}
```

#### 4. Journey Position Data

Add frames hold the following data:

```json5
{
  "frameType": "JOURNEY_POSITION",
  "updateType": "ADD",
  "frameData": {
    // the id of the journey (string, not null)
    "journeyId": "3bec7124-e6a9-583b-941d-77b3f5a27b1a",
    // the id of the server where the journey is active (string, not null)
    "serverId": "9db9b77d-a5ff-5385-89c3-6c6224e0824f",
    // the current speed of the journey (integer, not null)
    "speed": 102,
    // the steam id of the driver (string, null if bot drives train)
    "driverSteamId": "76561198342066455",
    // the category of the train (string, not null)
    "category": "EIJ",
    // the number of the train (string, not null)
    "number": "5411",
    // the id of the signal ahead of the train (string, null if signal is too far away)
    "nextSignalId": "DG_A",
    // the distance of the train to the signal ahead (integer, null if signal is too far away)
    "nextSignalDistance": 760,
    // the max speed displayed by the signal ahead (integer, null if Vmax is displayed)
    "nextSignalMaxSpeed": 0,
    // the current latitude where the train is located (double, not null)
    "positionLat": 50.33412170410156,
    // the current longitude where the train is located (double, not null)
    "positionLng": 19.204269409179688
  }
}
```

Update and delete frames hold the following data:

```json5
{
  "frameType": "JOURNEY_POSITION",
  "updateType": "UPDATE",
  "frameData": {
    // the id of the journey (string, not null)
    "journeyId": "3bec7124-e6a9-583b-941d-77b3f5a27b1a",
    // the current speed of the journey (integer, not present if unchanged)
    "speed": 102,
    // the steam id of the driver (string, not present if unchanged)
    "driverSteamId": "76561198342066455",
    // the id of the signal ahead of the train (string, not present if unchanged)
    "nextSignalId": "DG_A",
    // the distance of the train to the signal ahead (integer, not present if unchanged)
    "nextSignalDistance": 760,
    // the max speed displayed by the signal ahead (integer, not present if unchanged)
    "nextSignalMaxSpeed": 0,
    // the current latitude where the train is located (double, not present if unchanged)
    "positionLat": 50.33412170410156,
    // the current longitude where the train is located (double, not present if unchanged)
    "positionLng": 19.204269409179688
  }
}
```
