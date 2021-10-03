### About

This project works but is unfinished and probably will not receive much support.

RoomApi is a Paper plugin that supports performant "rooms", basically air blocks that are enclosed in a space.
The plugin can detect if the space is a room and has an api to communicate with an embedded database.
I tried to favor performance and use asynchronous calls for both the database and algorithms. I did my best to have good
thread safely, and I am pretty sure I did not do anything nasty.

### Dependencies

Only tested and supported Paper 1.17.1 and Java 16

### Install
#### Server
* Add the plugins jar to you server's plugin folder
#### Dev
* Clone the repo
* To build run `./gradlew shadowJar`

### Usage
* RoomApi uses callbacks that are run synchronously
#### Kotlin
```
// Creates a room with the current player's locations
// and uuid. This will happen asynchronously but the callback
// will in sync on the main thread.
RoomApi.CreateRoom(player) {
	Bukkit.getLogger().info("This is my Room Id! ${it.RoomId}")
}

// Uses the player's location to detect if we are in a room.
// If so the callback will be invoked with the current Room object.
RoomApi.GetRoomFromLocation(player.location) { room ->
	room.BlockLocations.forEach {
		Bukkit.getLogger().info("Looping through the rooms block ${it.block.type}")
	}
}
```