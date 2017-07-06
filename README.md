# uber-android
## Boiler plate uber app for android
Installation
```
Create a parse server app [Here](https://github.com/parse-community/parse-server-example)
```
Add dependency to the application level build.gradle file.
```
dependencies {
  compile 'com.parse:parse-android:1.15.7'
}
```
Edit the parse server path in 
```
Parse.initialize(new Parse.Configuration.Builder(this)
      .applicationId("YOUR_APP_ID")
      .server("http://localhost:1337/parse/")
      .build()
    );
```
