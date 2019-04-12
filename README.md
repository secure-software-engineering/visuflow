# VisuFlow

This project contains the main Eclipse plug-in of the VisuFlow project group. VisuFlow helps static code developers in writing static analyses on top of Soot.

# Installation

Import the `visuflow-plugin` and `visuflow-target` projects into your "Eclipse IDE for RCP and RAP Developers" workspace and then follow the steps below:

1. Open `visuflow.target` file in visuflow-target project.
2. Click on the link `Set as Target Platform` in the top right corner.
3. Open your run configuration for the plugin under `Run -> Run Configurations...`
4. Make sure that `org.eclipse.ui.ide.workbench` is selected under `Main -> Program to run -> Run an application`
5. Switch to tab `Plug-ins`
6. Select `plug-ins selected below only` in combobox
7. On the right click on `Deselect all`
8. Tick all plug-ins under `Workspace`
9. On the right click on `Add required Plug-ins`
10. Click on `Validate Plug-ins` -> Hopefully no problems ;)
11. `Apply`
12. `Run`

# Running the Analysis

After the plugin have been launched import the `visuflow-uitests-analysis` and `visuflow-uitests-target` projects from the DemoApp folder into the plugin workspace. Also check the option `Copy projects into workspace` while importing them.

1. Make sure to reference the correct soot jar file in the build path of the project `visuflow-uitests-analysis`.
2. Now follow the insturctions [in this video](https://www.youtube.com/watch?v=51iimUDaOPQ).
3. Trigger Stepping Backwards:
   - In VisuFlow’s graph view, it is possible to right-click any evaluated unit and to choose the option `stepback` while debugging the analysis.
   - If there are several potential methods to step back to, the user interface requests the user’s choice about the path to step back into.
   - This is shown as green arrow icons in the graph view, from which user can select the desired unit to step back to by click the desired arrow icon.
   - Another option for triggering the debugger to step back is by using the timeline view which can be opened using `Window > Show View > Other... > VisuFlow >      TimelineView`.

# Compatibility

The VisuFlow plugin is compatible with Java 8 and all the newer versions of Eclipse (Neon +).
For Java 9 the work is in progress.