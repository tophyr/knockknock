# Knockknock

A simple lock screen library that secures access to an application.

## Use

To use, simply set your app to use ```SecuredApplication``` as its Application class:

```xml
<application
            android:name="com.sarbs.knockknock.SecuredApplication">
    ...
    ...
```

If you already have an Application subclass, then just insert ```SecuredApplication``` into the inheritance chain
somewhere. Once that's done, call ```SecuredApplication.launchSetPasscodeActivity(Activity start)``` in order to allow
the user to set their PIN, and ```SecuredApplication.clearPasscode(Context ctx)``` to clear it. While a PIN is set,
the app will force the user to authenticate anytime they've left the app for more than ten seconds.

If you want a particular activity to be exempt from the PIN lock, make it implement the marker interface ```LockScreen``` .

## License

Licensed for any use under the Apache License, v2.0. Because, FGPL.
