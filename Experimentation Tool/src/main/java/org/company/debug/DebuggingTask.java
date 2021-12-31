package org.company.debug;

import org.company.jacoco.JaCoCoMethod;
import org.json.JSONObject;

import java.util.ArrayList;

import static org.company.debug.Const.kName;
import static org.company.debug.Const.kUnits;

/**
 * A class representing a debugging task. Mainly, the class is a wrapper for debugging task units
 */
public class DebuggingTask {

    /**
     * the list of debugging task units
     */
    private final ArrayList<DebuggingTaskUnit> debuggingTaskUnits;

    /**
     * the name given to this debugging task
     */
    private final String debuggingTaskName;

    /**
     * the index in the array of debugging task units of the current debugging task unit
     */
    private int index;

    /**
     * the method against which the debugging task runs to. This is needed
     * because some debugging tasks units are parametrized (e.g., set breakpoint in method)
     */
    private JaCoCoMethod methodToProtect;


    /**
     * simple constructor
     * @param debuggingTaskJSON the JSON array containing the debugging task units
     * @param methodToProtect the method against which the debugging task runs to
     *                        this is needed because some debugging tasks units are
     *                        parametrized (e.g., set breakpoint in method)
     */
    public DebuggingTask(JSONObject debuggingTaskJSON, JaCoCoMethod methodToProtect) {

        this.methodToProtect    = methodToProtect;
        this.debuggingTaskName  = debuggingTaskJSON.getString(kName);

        debuggingTaskUnits = new ArrayList<>();

        debuggingTaskJSON.getJSONArray(kUnits).forEach(debuggingTaskUnitJSON -> {
                JSONObject currentDebuggingTaskUnitJSON = (JSONObject) debuggingTaskUnitJSON;
                DebuggingTaskUnit currentDebuggingTaskUnit = new DebuggingTaskUnit(currentDebuggingTaskUnitJSON, methodToProtect);
                debuggingTaskUnits.add(currentDebuggingTaskUnit);
            }
        );

        index = 0;
    }

    /**
     * return the current debugging task and moves to the next one, if any
     * @return the current debugging task, or null if no tasks are available
     */
    public DebuggingTaskUnit next() {
        if (areThereMoreDebuggingTasksUnits())
            return debuggingTaskUnits.get(index++);
        else
            return null;
    }

    /**
     * check whether there are still debugging task units or not
     * @return true if there are still debugging task units, false otherwise
     */
    public boolean areThereMoreDebuggingTasksUnits() {
        return index < debuggingTaskUnits.size();
    }

    /**
     * wrapper to allow to add debugging task units
     * @param index where to add the debugging task
     * @param debuggingTaskUnit the debugging task
     */
    public void addDebuggingTaskUnit(int index, DebuggingTaskUnit debuggingTaskUnit) {
        debuggingTaskUnits.add(index, debuggingTaskUnit);
    }

    /**
     * getter for the name of this debugging task
     * @return the name of this debugging task
     */
    public String getDebuggingTaskName() {
        return debuggingTaskName;
    }

    /**
     * getter for the method to protect
     * @return the method to protect
     */
    public JaCoCoMethod getMethodToProtect() {
        return methodToProtect;
    }


    /**
     * reset the index
     */
    public void reset() {
        index = 0;
    }
}
