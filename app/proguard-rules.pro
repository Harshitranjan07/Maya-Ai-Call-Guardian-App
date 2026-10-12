# Keep Room entities and DAOs
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep data models used in JSON serialization and AI structured events
-keepclassmembers class * {
    @androidx.room.Entity *;
}
