# Native-code debugger integration for IntelliJ IDEA

## Building

    ./gradlew :buildPlugin

# Installation

Add `./build/distributions/idea-native2-debugger-1.0-SNAPSHOT.zip` to IntelliJ IDEA plugins. To to that, press Ctrl-Alt-S, then go to `Plugins`, then click the gear icon and then go to `Install Plugin from Disk...`.

# Running

In order to run your program under the debugger for the first time, go to `Run` / `Edit configurations...`, then add a configuration using the `+` sign and choosing `Native2Debugger`. 

From then on, choose this configuration in order to make the plugin run your program under `gdb`.

Note that this currently does NOT build your project first, so make sure to build it when you want to.

There are further settings available in Ctrl-Alt-S, `Build, Execution, Deployment`, `Native2 Debugger`. You probably don't need to change those--but you can.
