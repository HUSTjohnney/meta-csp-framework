/*******************************************************************************
 * Copyright (c) 2010-2013 Federico Pecora <federico.pecora@oru.se>
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ******************************************************************************/
package org.metacsp.meta.simplePlanner;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import org.metacsp.framework.Constraint;
import org.metacsp.framework.ConstraintNetwork;
import org.metacsp.framework.ConstraintSolver;
import org.metacsp.framework.ValueOrderingH;
import org.metacsp.framework.Variable;
import org.metacsp.framework.VariableOrderingH;
import org.metacsp.framework.VariablePrototype;
import org.metacsp.framework.meta.MetaConstraint;
import org.metacsp.framework.meta.MetaConstraintSolver;
import org.metacsp.framework.meta.MetaVariable;
import org.metacsp.meta.hybridPlanner.FluentBasedSimpleDomain;
import org.metacsp.meta.symbolsAndTime.Schedulable;
import org.metacsp.multi.activity.SymbolicVariableActivity;
import org.metacsp.multi.activity.ActivityNetworkSolver;
import org.metacsp.multi.allenInterval.AllenIntervalConstraint;
import org.metacsp.spatial.utility.SpatialRule;
import org.metacsp.time.Bounds;

public class SimpleDomain extends MetaConstraint {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5143516447467774523L;
	protected Vector<SimpleOperator> operators;
	protected String[] resourceNames;
	protected HashMap<String,SimpleReusableResource> resourcesMap;
	protected HashMap<SimpleReusableResource,HashMap<Variable,Integer>> currentResourceUtilizers;
	private String everything = null;
	protected long filteringTime = Long.MIN_VALUE;
	private String name;

	protected Vector<String> sensors = new Vector<String>();
	protected Vector<String> actuators = new Vector<String>();
	protected Vector<String> contextVars = new Vector<String>();
	protected HashMap<SimpleOperator, Integer> operatorsLevels = new HashMap<SimpleOperator, Integer>(); 
	
	public HashMap<SymbolicVariableActivity, SymbolicVariableActivity> unificationTrack = new HashMap<SymbolicVariableActivity,SymbolicVariableActivity>();
    private Vector<String> timelines = new Vector<String>();

    public String[] getTimelines() {
        return this.timelines.toArray(new String[this.timelines.size()]);
    }

    public enum markings {UNJUSTIFIED, JUSTIFIED, DIRTY, STATIC, IGNORE, PLANNED, UNPLANNED, PERMANENT, OBSERVED_UNJ, OBSERVED_JUST, IMPOSSIBLE,
		COND_UNJUSTIFIED, COND_CURRENT_UNJUSTIFIED};

	public Schedulable[] getSchedulingMetaConstraints() {
		return currentResourceUtilizers.keySet().toArray(new Schedulable[currentResourceUtilizers.keySet().size()]);
	}
	
	public HashMap<SymbolicVariableActivity, SymbolicVariableActivity> getUnificationTrack(){
		return unificationTrack;
	}
	
	public void setFileteringTime(long filteringTime) {
		this.filteringTime = filteringTime;
	}
	
	public SimpleDomain(int[] capacities, String[] resourceNames, String domainName) {
		super(null, null);
		this.name = domainName;
		this.resourceNames = resourceNames;
		currentResourceUtilizers = new HashMap<SimpleReusableResource,HashMap<Variable,Integer>>();
		resourcesMap = new HashMap<String, SimpleReusableResource>();
		operators = new Vector<SimpleOperator>();

		for (int i = 0; i < capacities.length; i++) {
			//Most critical conflict is the one with most activities 
			VariableOrderingH varOH = new VariableOrderingH() {
				@Override
				public int compare(ConstraintNetwork arg0, ConstraintNetwork arg1) {
					return arg1.getVariables().length - arg0.getVariables().length;
				}
				@Override
				public void collectData(ConstraintNetwork[] allMetaVariables) { }
			};
			// no value ordering
			ValueOrderingH valOH = new ValueOrderingH() {
				@Override
				public int compare(ConstraintNetwork o1, ConstraintNetwork o2) { return 0; }
			};
			resourcesMap.put(resourceNames[i], new SimpleReusableResource(varOH, valOH, capacities[i], this, resourceNames[i]));
		}

		// for every SRR just created, couple it with a vector of variables
		for (SimpleReusableResource rr : resourcesMap.values()) currentResourceUtilizers.put(rr,new HashMap<Variable, Integer>());
	}
	
	protected SimpleDomain(int[] capacities, String[] resourceNames, String domainName, String everything) {
		this(capacities, resourceNames, domainName);
		this.everything = everything;
	}
	
	public void addResourceUtilizers(SimpleReusableResource rr, HashMap<Variable, Integer> hm) {
		currentResourceUtilizers.put(rr,hm);
	}

	public void addResourceUtilizer(SimpleReusableResource rr, Variable var, Integer amount) {
		currentResourceUtilizers.get(rr).put(var,amount);
	}
	
	public void addResourceMap(String resourcename, SimpleReusableResource simpleReusableResource){
		resourcesMap.put(resourcename, simpleReusableResource);
	}
	
	public void addOperator(SimpleOperator r) {
		operators.add(r);
	}

	public SimpleOperator[] getOperators() {
		return operators.toArray(new SimpleOperator[operators.size()]);
	}		

	@Override
	public ConstraintSolver getGroundSolver() {
		return (ActivityNetworkSolver)this.metaCS.getConstraintSolvers()[0];
	}

	@Override
	public ConstraintNetwork[] getMetaVariables() {
		ActivityNetworkSolver groundSolver = (ActivityNetworkSolver)getGroundSolver();//(ActivityNetworkSolver)this.metaCS.getConstraintSolvers()[0];
		Vector<ConstraintNetwork> ret = null;
		// for every variable that is marked as UNJUSTIFIED a ConstraintNetwork is built
		for (Variable task : groundSolver.getVariables()) {
			if (task.getMarking().equals(markings.UNJUSTIFIED)) {
				ConstraintNetwork nw = new ConstraintNetwork(null);
				nw.addVariable(task);
				if (ret == null) ret = new Vector<ConstraintNetwork>();
				ret.add(nw);
			}
		}
		if (ret == null) return null;
		return ret.toArray(new ConstraintNetwork[ret.size()]);
	}

	
	protected ConstraintNetwork expandOperator(SimpleOperator possibleOperator, SymbolicVariableActivity problematicActivity) {
		ConstraintNetwork activityNetworkToReturn = new ConstraintNetwork(null);
		ActivityNetworkSolver groundSolver = (ActivityNetworkSolver)getGroundSolver();

		String possibleOperatorHead = possibleOperator.getHead();
		String possibleOperatorHeadSymbol = possibleOperatorHead.substring(possibleOperatorHead.indexOf("::")+2, possibleOperatorHead.length());
		String possibleOperatorHeadComponent = possibleOperatorHead.substring(0, possibleOperatorHead.indexOf("::"));
		Variable headActivity = null;

		boolean problematicActIsEffect = false;
		Variable[] operatorTailActivitiesToInsert = new Variable[0];

		if (possibleOperator.getRequirementActivities() != null) {
			operatorTailActivitiesToInsert = new Variable[possibleOperator.getRequirementActivities().length];

			for (int i = 0; i < possibleOperator.getRequirementActivities().length; i++) {
				String possibleOperatorTail = possibleOperator.getRequirementActivities()[i];
				String possibleOperatorTailComponent = possibleOperatorTail.substring(0, possibleOperatorTail.indexOf("::"));				
				String possibleOperatorTailSymbol = possibleOperatorTail.substring(possibleOperatorTail.indexOf("::")+2, possibleOperatorTail.length());

				//If this req is the prob act, then insert prob act
				if (possibleOperatorTailComponent.equals(problematicActivity.getComponent()) && possibleOperatorTailSymbol.equals(problematicActivity.getSymbolicVariable().getSymbols()[0])) {
					operatorTailActivitiesToInsert[i] = problematicActivity;
					problematicActIsEffect = true;
				}
				//else make a new var prototype and insert it
				else {
					VariablePrototype tailActivity = new VariablePrototype(groundSolver, possibleOperatorTailComponent, possibleOperatorTailSymbol);
					operatorTailActivitiesToInsert[i] = tailActivity;
					if (possibleOperator instanceof PlanningOperator) {
						if (((PlanningOperator)possibleOperator).isEffect(possibleOperatorTail)) {
							tailActivity.setMarking(markings.JUSTIFIED);
						}
						else {
							tailActivity.setMarking(markings.UNJUSTIFIED);
						}
					}
					else {
						tailActivity.setMarking(markings.UNJUSTIFIED);
					}
				}
			}

			//Also add head if the prob activity was unified with an effect
			if (problematicActIsEffect) {
				headActivity = new VariablePrototype(groundSolver, possibleOperatorHeadComponent, possibleOperatorHeadSymbol);
				headActivity.setMarking(markings.JUSTIFIED);
			}

			Vector<AllenIntervalConstraint> allenIntervalConstraintsToAdd = new Vector<AllenIntervalConstraint>();

			for (int i = 0; i < possibleOperator.getRequirementConstraints().length; i++) {
				if (possibleOperator.getRequirementConstraints()[i] != null) {
					AllenIntervalConstraint con = (AllenIntervalConstraint)possibleOperator.getRequirementConstraints()[i].clone();
					if (problematicActIsEffect) con.setFrom(headActivity);
					else con.setFrom(problematicActivity);
					con.setTo(operatorTailActivitiesToInsert[i]);
					allenIntervalConstraintsToAdd.add(con);
				}
			}
			for (AllenIntervalConstraint con : allenIntervalConstraintsToAdd) activityNetworkToReturn.addConstraint(con);
		}

		Vector<AllenIntervalConstraint> toAddExtra = new Vector<AllenIntervalConstraint>();
		for (int i = 0; i < operatorTailActivitiesToInsert.length+1; i++) {
			AllenIntervalConstraint[][] ec = possibleOperator.getExtraConstraints();
			if (ec != null) {
				AllenIntervalConstraint[] con = ec[i];
				for (int j = 0; j < con.length; j++) {
					if (con[j] != null) {
						AllenIntervalConstraint newCon = (AllenIntervalConstraint) con[j].clone();
						if (i == 0) {
							if (problematicActIsEffect) newCon.setFrom(headActivity);
							else newCon.setFrom(problematicActivity);
						}
						else {
							newCon.setFrom(operatorTailActivitiesToInsert[i-1]);
						}
						if (j == 0) {
							if (problematicActIsEffect) newCon.setTo(headActivity);
							else newCon.setTo(problematicActivity);
						}
						else {
							newCon.setTo(operatorTailActivitiesToInsert[j-1]);
						}
						toAddExtra.add(newCon);
					}
				}
			}
		}

		for (Variable v : operatorTailActivitiesToInsert) activityNetworkToReturn.addVariable(v);
		if (!toAddExtra.isEmpty()) {
			for (AllenIntervalConstraint con : toAddExtra) activityNetworkToReturn.addConstraint(con);
		}

		int[] usages = possibleOperator.getUsages();
		if (usages != null) {
			for (int i = 0; i < usages.length; i++) {
				if (usages[i] != 0) {
					HashMap<Variable, Integer> utilizers = currentResourceUtilizers.get(resourcesMap.get(resourceNames[i]));
					if (problematicActIsEffect) utilizers.put(headActivity, usages[i]);
					else utilizers.put(problematicActivity, usages[i]);
					activityNetworkToReturn.addVariable(problematicActivity);
				}
			}
		}
		return activityNetworkToReturn;						
	}

	public void addSensor(String sensor) {
		this.sensors.add(sensor);
	}
	
	public String[] getSensors() {
		return this.sensors.toArray(new String[this.sensors.size()]);
	}

	public void addActuator(String actuator) {
		this.actuators.add(actuator);
	}

	public String[] getActuators() {
		return this.actuators.toArray(new String[this.actuators.size()]);
	}

	public void addContextVar(String cv) {
		this.contextVars.add(cv);
	}
	
	public String[] getContextVars() {
		return this.contextVars.toArray(new String[this.contextVars.size()]);
	}

	public boolean isSensor(String component) {
		if (sensors.contains(component)) return true;
		return false;
	}
		
	public boolean isActuator(String component) {
		if (actuators.contains(component)) return true;
		return false;
	}

	public boolean isContextVar(String component) {
		if (contextVars.contains(component)) return true;
		return false;
	}

	private Vector<SymbolicVariableActivity> filterUnifications(Vector<SymbolicVariableActivity> possibleUnifications) {
		Vector<SymbolicVariableActivity> ret = new Vector<SymbolicVariableActivity>();
		for (SymbolicVariableActivity act : possibleUnifications) {
			if (act.getTemporalVariable().getLET() >= filteringTime) {
				ret.add(act);
			}
		}
		return ret;
	}
	
	protected ConstraintNetwork[] getUnifications(SymbolicVariableActivity activity) {
		ActivityNetworkSolver groundSolver = (ActivityNetworkSolver)getGroundSolver();//(ActivityNetworkSolver)this.metaCS.getConstraintSolvers()[0];
		Variable[] acts = groundSolver.getVariables();
				
		Vector<SymbolicVariableActivity> possibleUnifications = new Vector<SymbolicVariableActivity>();
		for (Variable var : acts) {
			if (!var.equals(activity)) {
				SymbolicVariableActivity act = (SymbolicVariableActivity)var;
				String problematicActivitySymbolicDomain = activity.getSymbolicVariable().getSymbols()[0];
				if (act.getComponent().equals(activity.getComponent())) {
					String[] actSymbols = act.getSymbolicVariable().getSymbols();
					for (String symbol : actSymbols) {
						if (problematicActivitySymbolicDomain.contains(symbol)) {
							if (act.getMarking().equals(markings.JUSTIFIED)) {
								possibleUnifications.add(act);
							}
							break;
						}
					}
				}
			}
		}
		//return getUnifications(activity,possibleUnifications);
		return getUnifications(activity,filterUnifications(possibleUnifications));
	}
	
	private ConstraintNetwork[] getUnifications(SymbolicVariableActivity activity, Vector<SymbolicVariableActivity> possibleUnifications) {
		Vector<ConstraintNetwork> unifications = new Vector<ConstraintNetwork>();
		for (SymbolicVariableActivity act : possibleUnifications) {
			ConstraintNetwork oneUnification = new ConstraintNetwork(null);
			AllenIntervalConstraint equals = new AllenIntervalConstraint(AllenIntervalConstraint.Type.Equals);
			equals.setFrom(activity);
			equals.setTo(act);
			oneUnification.addConstraint(equals);
//			SymbolicValueConstraint eqValue = new SymbolicValueConstraint(SymbolicValueConstraint.Type.EQUALS);
//			eqValue.setFrom(activity);
//			eqValue.setTo(act);
//			oneUnification.addConstraint(eqValue);
			unifications.add(oneUnification);
            //highest priority
            oneUnification.setAnnotation(2);
		}
		if (unifications.isEmpty()) return null;
		return unifications.toArray(new ConstraintNetwork[unifications.size()]);
	}

	@Override
	public ConstraintNetwork[] getMetaValues(MetaVariable metaVariable) {
		Vector<ConstraintNetwork> retPossibleConstraintNetworks = new Vector<ConstraintNetwork>();
		ConstraintNetwork problematicNetwork = metaVariable.getConstraintNetwork();
		SymbolicVariableActivity problematicActivity = (SymbolicVariableActivity)problematicNetwork.getVariables()[0];
		
		logger.finest("Getting metavalues for " + problematicActivity);
		
		Vector<ConstraintNetwork> operatorsConsNetwork = new Vector<ConstraintNetwork>();
		Vector<ConstraintNetwork> unificationConsNetwork = new Vector<ConstraintNetwork>();
		
		//If it's a sensor, it needs to be unified
		if (isSensor(problematicActivity.getComponent())) {
			logger.finest(problematicActivity.getComponent() + " is a Sensor - adding unifications");
			ConstraintNetwork[] unifications = this.getUnifications(problematicActivity);
			if (unifications != null)
				for (ConstraintNetwork cn : unifications) cn.setAnnotation(2);
            //But before returning all the unifications (which could be the empty set),
            // also add the expansions of PlanningOperators that have this problematic
            // activity as an AchievedState
            String problematicActivitySymbolicDomain = problematicActivity.getSymbolicVariable().getSymbols()[0];
            for (SimpleOperator r : operators) {
                if (r instanceof PlanningOperator) {
                    for (String reqState : r.getRequirementActivities()) {
                        String operatorEffect = reqState;
                        String opeatorEffectComponent = operatorEffect.substring(0, operatorEffect.indexOf("::"));
                        String operatorEffectSymbol = operatorEffect.substring(operatorEffect.indexOf("::")+2, operatorEffect.length());
                        if (((PlanningOperator)r).isEffect(reqState)) {
                            if(problematicActivity.getComponent().equals(opeatorEffectComponent) ) {
                                if(problematicActivitySymbolicDomain.equals(operatorEffectSymbol)) {
                                    ConstraintNetwork newResolver = expandOperator(r,problematicActivity);
                                    newResolver.annotation = r;
                                    //middle priority
                                    newResolver.setAnnotation(1);
                                    retPossibleConstraintNetworks.add(newResolver);

                                }
                            }
                        }
                    }
                }

            }
            if (null != unifications)  {
                retPossibleConstraintNetworks.addAll(Arrays.asList(unifications));
            }
			return retPossibleConstraintNetworks.toArray(new ConstraintNetwork[retPossibleConstraintNetworks.size()]);
		}
						
		//Find all expansions
		for (SimpleOperator r : operators) {
			String problematicActivitySymbolicDomain = problematicActivity.getSymbolicVariable().getSymbols()[0];
			String operatorHead = r.getHead();
			String opeatorHeadComponent = operatorHead.substring(0, operatorHead.indexOf("::"));
			String operatorHeadSymbol = operatorHead.substring(operatorHead.indexOf("::")+2, operatorHead.length());
			if (opeatorHeadComponent.equals(problematicActivity.getComponent())) {
				if (problematicActivitySymbolicDomain.contains(operatorHeadSymbol)) {
					ConstraintNetwork newResolver = expandOperator(r,problematicActivity);
					//middle priority
					newResolver.setAnnotation(1);
					newResolver.setSpecilizedAnnotation(r);
					operatorsConsNetwork.add(newResolver);
				}
			}
			
			if (r instanceof PlanningOperator) {
				for (String reqState : r.getRequirementActivities()) {
					String operatorEffect = reqState;
					String opeatorEffectComponent = operatorEffect.substring(0, operatorEffect.indexOf("::"));
					String operatorEffectSymbol = operatorEffect.substring(operatorEffect.indexOf("::")+2, operatorEffect.length());
					if (((PlanningOperator)r).isEffect(reqState)) {
						if (opeatorEffectComponent.equals(problematicActivity.getComponent())) {
							if (problematicActivitySymbolicDomain.contains(operatorEffectSymbol)) {
								ConstraintNetwork newResolver = expandOperator(r,problematicActivity);
								newResolver.annotation = r;
								//middle priority
								newResolver.setAnnotation(1);
								retPossibleConstraintNetworks.add(newResolver);
							}
						}
					}
				}
			}
		}
		
		logger.finest(problematicActivity.getComponent() + " is not a Sensor - adding expansions");
		
		//If it's a context var, it needs to be unified (or expanded, see above) 
		if (isContextVar(problematicActivity.getComponent())) {
			//System.out.println("CONTEXTVAR: " + problematicActivity.getComponent());
			logger.finest(problematicActivity.getComponent() + " is a ContextVariable - adding unifications");
			ConstraintNetwork[] unifications = getUnifications(problematicActivity);
			if (unifications != null) {
				for (ConstraintNetwork oneUnification : unifications) {
					retPossibleConstraintNetworks.add(oneUnification);
					//highest priority
					oneUnification.setAnnotation(2);
				}
			}
		}
		
		//If it's a context var, it needs to be unified (or expanded, see above)
		else if (isActuator(problematicActivity.getComponent())) {
			//System.out.println("ACTUATOR: " + problematicActivity.getComponent());
			logger.finest(problematicActivity.getComponent() + " is an Actuator - adding unifications");
			ConstraintNetwork[] unifications = getUnifications(problematicActivity);
			if (unifications != null) {
				for (ConstraintNetwork oneUnification : unifications) {
					retPossibleConstraintNetworks.add(oneUnification);
					//highest priority
					oneUnification.setAnnotation(2);
				}
			}
		}

		retPossibleConstraintNetworks.addAll(unificationConsNetwork);
		retPossibleConstraintNetworks.addAll(operatorsConsNetwork);				
		
		if (operatorsConsNetwork.isEmpty()) {
			//Actuator, but no expansions available - so justified by default
			if (isActuator(problematicActivity.getComponent())) {
				logger.finest(problematicActivity.getComponent() + " is an Actuator but has no available expansions - activity is directly supported");
				ConstraintNetwork nullActivityNetwork = new ConstraintNetwork(null);
				nullActivityNetwork.setSpecilizedAnnotation(false);
				//least priority
				nullActivityNetwork.setAnnotation(0);
				retPossibleConstraintNetworks.add(nullActivityNetwork);
			}
		}
		
		if (!retPossibleConstraintNetworks.isEmpty()) {
			return retPossibleConstraintNetworks.toArray(new ConstraintNetwork[retPossibleConstraintNetworks.size()]);
		}
		logger.finest(problematicActivity.getComponent() + " HAS NO RESOLVERS, will FAIL!");
		return null;
	}

	@Override
	public void markResolvedSub(MetaVariable con, ConstraintNetwork metaValue) {
		if (con.getConstraintNetwork().getVariables().length != 0)
			con.getConstraintNetwork().getVariables()[0].setMarking(markings.JUSTIFIED);
	}

	@Override
	public void draw(ConstraintNetwork network) {
		// TODO Auto-generated method stub	
	}

	public HashMap<String, SimpleReusableResource> getResources() {
		return resourcesMap;
	}

	// Given a variable act, it returns all the resources that are currently exploited by the variable
	public SimpleReusableResource[] getCurrentReusableResourcesUsedByActivity(Variable act) {
		Vector<SimpleReusableResource> ret = new Vector<SimpleReusableResource>();
		for (SimpleReusableResource rr : currentResourceUtilizers.keySet()) {
			if (currentResourceUtilizers.get(rr).containsKey(act)) 
				ret.add(rr);
		}
		return ret.toArray(new SimpleReusableResource[ret.size()]);
	}

	public int getResourceUsageLevel(SimpleReusableResource rr, Variable act) {
		return currentResourceUtilizers.get(rr).get(act);
	}
	
	public HashMap<SimpleReusableResource,HashMap<Variable,Integer>> getAllResourceUsageLevel(){
		return currentResourceUtilizers;
	}

	public void resetAllResourceAllocation(){
		currentResourceUtilizers = new HashMap<SimpleReusableResource, HashMap<Variable,Integer>>();
		for (SimpleReusableResource rr : resourcesMap.values()) currentResourceUtilizers.put(rr,new HashMap<Variable, Integer>());

	}
	
	@Override
	public String toString() {
		String ret = this.getClass().getSimpleName() + " " + this.name;
		//		ret += "\nResources:\n";
		//		for (SimpleReusableResource rr : resourcesMap.values())
		//			ret += "  " + rr + "\n";
		//		for (SimpleOperator op : operators) {
		//			ret += "--- Operator:\n";
		//			ret += op + "\n";
		//		}
		return ret;
	}

	@Override
	public String getEdgeLabel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object clone() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isEquivalent(Constraint c) {
		// TODO Auto-generated method stub
		return false;
	}

	public static String instantiateVariable(String var) {
		return var.substring(var.indexOf('?'));
	}
	
	/**
	 * Creates a {@link SimpleOperator} from a textual specification (used by the
	 * domain parser).
	 * @param textualSpecification A textual specification of an operator
	 * @param resources The resources (identifiers of {@link SimpleReusableResource}s) used in this operator.
	 * @param planningOp Whether this is a {@link PlanningOperator} or a {@link SimpleOperator}.
	 * @return A {@link SimpleOperator} build according to the textual specification.
	 */
	private static SimpleOperator parseOperator(String textualSpecification, String[] resources, boolean planningOp) {
		HashMap<String,String> requiredStates = new HashMap<String, String>();
		String head = null;
		Vector<AllenIntervalConstraint> constraints = new Vector<AllenIntervalConstraint>();
		Vector<String> froms = new Vector<String>();
		Vector<String> tos = new Vector<String>();
		int[] resourceRequirements = new int[resources.length];
		HashMap<String,Boolean> effects = new HashMap<String, Boolean>();

		String[] headElement = parseKeyword("Head", textualSpecification);
		head = headElement[0].trim();

		String[] requiredStateElements = parseKeyword("RequiredState", textualSpecification);
		for (String reqElement : requiredStateElements) {
			String reqKey = reqElement.substring(0,reqElement.indexOf(" ")).trim();
			String reqState = reqElement.substring(reqElement.indexOf(" ")).trim();
			requiredStates.put(reqKey, reqState);
			effects.put(reqKey,false);
		}

		String[] achievedStateElements = parseKeyword("AchievedState", textualSpecification);
		for (String achElement : achievedStateElements) {
			String achKey = achElement.substring(0,achElement.indexOf(" ")).trim();
			String achState = achElement.substring(achElement.indexOf(" ")).trim();
			requiredStates.put(achKey, achState);
			effects.put(achKey,true);
		}

		String[] constraintElements = parseKeyword("Constraint", textualSpecification);
		for (String conElement : constraintElements) {
			String constraintName = null;
			Vector<Bounds> bounds = null;
			if (conElement.contains("[")) {
				constraintName = conElement.substring(0,conElement.indexOf("[")).trim();
				String boundsString = conElement.substring(conElement.indexOf("["),conElement.lastIndexOf("]")+1);
				String[] splitBounds = boundsString.split("\\[");
				bounds = new Vector<Bounds>();
				for (String oneBound : splitBounds) {
					if (!oneBound.trim().equals("")) {
						String lbString = oneBound.substring(oneBound.indexOf("[")+1,oneBound.indexOf(",")).trim();
						String ubString = oneBound.substring(oneBound.indexOf(",")+1,oneBound.indexOf("]")).trim();
						long lb, ub;
						if (lbString.equals("INF")) lb = org.metacsp.time.APSPSolver.INF;
						else if (lbString.startsWith("?")) lb = Long.parseLong(instantiateVariable(lbString));
						else lb = Long.parseLong(lbString);
						if (ubString.equals("INF")) ub = org.metacsp.time.APSPSolver.INF;
						else if (ubString.startsWith("?")) ub = Long.parseLong(instantiateVariable(ubString));
						else ub = Long.parseLong(ubString);
						bounds.add(new Bounds(lb,ub));
					}
				}
			}
			else {
				constraintName = conElement.substring(0,conElement.indexOf("(")).trim();
			}
			String from = null;
			String to = null;
			String fromSeg = null;
			if (constraintName.equals("Duration")) {
				from = conElement.substring(conElement.indexOf("(")+1, conElement.indexOf(")")).trim();
				to = from;
			}
			else {
				fromSeg = conElement.substring(conElement.indexOf("("));
				from = fromSeg.substring(fromSeg.indexOf("(")+1, fromSeg.indexOf(",")).trim();
				to = fromSeg.substring(fromSeg.indexOf(",")+1, fromSeg.indexOf(")")).trim();
			}

			AllenIntervalConstraint con = null;
			if (bounds != null) {
				con = new AllenIntervalConstraint(AllenIntervalConstraint.Type.valueOf(constraintName),bounds.toArray(new Bounds[bounds.size()]));
			}
			else con = new AllenIntervalConstraint(AllenIntervalConstraint.Type.valueOf(constraintName));
			constraints.add(con);
			froms.add(from);
			tos.add(to);
		}

		String[] resourceElements = parseKeyword("RequiredResource", textualSpecification);
		for (String resElement : resourceElements) {
			String requiredResource = resElement.substring(0,resElement.indexOf("(")).trim();
			int requiredAmount = Integer.parseInt(resElement.substring(resElement.indexOf("(")+1,resElement.indexOf(")")).trim());
			for (int k = 0; k < resources.length; k++) {
				if (resources[k].equals(requiredResource)) {
					resourceRequirements[k] = requiredAmount;
				}
			}

		}

		class AdditionalConstraint {
			AllenIntervalConstraint con;
			int from, to;
			public AdditionalConstraint(AllenIntervalConstraint con, int from, int to) {
				this.con = con;
				this.from = from;
				this.to = to;
			}
			public void addAdditionalConstraint(SimpleOperator op) {
				op.addConstraint(con, from, to);
			}
		}

		//What I have:
		//constraints = {During, Duration, Before}
		//froms = {Head, Head, req1}
		//tos = {req1, Head, req2}
		//requirements = {req2 = Robot1::At(room), req1 = Robot1::MoveTo()}

		//pass this to constructor
		String[] requirementStrings = new String[requiredStates.keySet().size()];
		boolean[] effectBools = new boolean[requiredStates.keySet().size()];
		AllenIntervalConstraint[] consFromHeadtoReq = new AllenIntervalConstraint[requiredStates.keySet().size()];
		//Vector<AllenIntervalConstraint> consFromHeadToReq = new Vector<AllenIntervalConstraint>();
		Vector<AdditionalConstraint> acs = new Vector<AdditionalConstraint>();
		HashMap<String,Integer> reqKeysToIndices = new HashMap<String, Integer>();

		int reqCounter = 0;
		for (String reqKey : requiredStates.keySet()) {
			String requirement = requiredStates.get(reqKey);
			requirementStrings[reqCounter] = requirement;
			reqKeysToIndices.put(reqKey,reqCounter);
			if (planningOp) {
				if (effects.get(reqKey)) effectBools[reqCounter] = true;
				else effectBools[reqCounter] = false;
			}
			reqCounter++;
		}

		for (int i = 0; i < froms.size(); i++) {
			//Head -> Head
			if (froms.elementAt(i).equals("Head") && tos.elementAt(i).equals("Head")) {
				AdditionalConstraint ac = new AdditionalConstraint(constraints.elementAt(i), 0, 0);
				acs.add(ac);
			}
			//req -> req
			else if (!froms.elementAt(i).equals("Head") && !tos.elementAt(i).equals("Head")) {
				String reqFromKey = froms.elementAt(i);
				String reqToKey = tos.elementAt(i);
				int reqFromIndex = reqKeysToIndices.get(reqFromKey);
				int reqToIndex = reqKeysToIndices.get(reqToKey);
				AllenIntervalConstraint con = constraints.elementAt(i);
				AdditionalConstraint ac = new AdditionalConstraint(con, reqFromIndex+1, reqToIndex+1);
				acs.add(ac);
			}
			//req -> Head
			else if (!froms.elementAt(i).equals("Head") && tos.elementAt(i).equals("Head")) {
				String reqFromKey = froms.elementAt(i);
				int reqFromIndex = reqKeysToIndices.get(reqFromKey);
				AllenIntervalConstraint con = constraints.elementAt(i);
				AdditionalConstraint ac = new AdditionalConstraint(con, reqFromIndex+1, 0);
				acs.add(ac);
			}
			//Head -> req
			else if (froms.elementAt(i).equals("Head") && !tos.elementAt(i).equals("Head")) {
				AllenIntervalConstraint con = constraints.elementAt(i);
				String reqToKey = tos.elementAt(i);
				consFromHeadtoReq[reqKeysToIndices.get(reqToKey)] = con;
			}
		}

		//Call constructor
		SimpleOperator ret = null;
		if (!planningOp) ret = new SimpleOperator(head,consFromHeadtoReq,requirementStrings,resourceRequirements);
		else ret = new PlanningOperator(head,consFromHeadtoReq,requirementStrings,effectBools,resourceRequirements);
		for (AdditionalConstraint ac : acs) ac.addAdditionalConstraint(ret);
		return ret;
	}
	
	protected static String[] parseKeyword(String keyword, String everything) {
		Vector<String> elements = new Vector<String>();
		int lastElement = everything.lastIndexOf(keyword);
		while (lastElement != -1) {
			int bw = lastElement;
			int fw = lastElement;
			boolean skip = false;
			while (everything.charAt(--bw) != '(') { 
				if (everything.charAt(bw) != ' ' && everything.charAt(bw) != '(') {
					everything = everything.substring(0,bw);
					lastElement = everything.lastIndexOf(keyword);
					skip = true;
					break;
				}
			}
			if (!skip) {
				int parcounter = 1;
				while (parcounter != 0) {
					if (everything.charAt(fw) == '(') parcounter++;
					else if (everything.charAt(fw) == ')') parcounter--;
					fw++;
				}
				String element = everything.substring(bw,fw).trim();
				element = element.substring(element.indexOf(keyword)+keyword.length(),element.lastIndexOf(")")).trim();
				if (!element.startsWith(",") && !element.trim().equals("")) elements.add(element);
				everything = everything.substring(0,bw);
				lastElement = everything.lastIndexOf(keyword);
			}
		}
		return elements.toArray(new String[elements.size()]);		
	}

	protected static HashMap<String,Integer> processResources (String[] resources) {
		HashMap<String, Integer> ret = new HashMap<String, Integer>();
		for (String resourceElement : resources) {
			String resourceName = resourceElement.substring(0,resourceElement.indexOf(" ")).trim();
			int resourceCap = Integer.parseInt(resourceElement.substring(resourceElement.indexOf(" ")).trim());
			ret.put(resourceName, resourceCap);
		}
		return ret;
	}

	/**
	 * Parse a user-defined keyword in the domain, in the form <code>(UserKeyword value1 [value2 ... valueN])</code>
	 * @param keyword The keyword to parse, i.e., <code>UserKeyword</code>
	 * @return An array of value lists, one for each element with the user keyword, e.g., <code>["value11 value12", "value21 value22 value23", "value31", ...]</code>
	 */
	public String[] parseUserKeyword(String keyword) {
		return parseKeyword(keyword, this.everything);
	}

	/**
	 * Parses a domain file (see domains/testDomain.ddl for an example), instantiates
	 * the necessary {@link MetaConstraint}s and adds them to the provided {@link SimplePlanner}.
	 * @param sp The {@link SimplePlanner} that will use this domain.
	 * @param filename Text file containing the domain definition. 
	 */
	public static SimpleDomain parseDomain(MetaConstraintSolver sp, String filename, Class<?> domainType) {
		String everything = null;
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			try {
				StringBuilder sb = new StringBuilder();
				String line = br.readLine();
				while (line != null) {
					if (!line.trim().startsWith("#")) {
						sb.append(line);
						sb.append('\n');
					}
					line = br.readLine();
				}
				everything = sb.toString();
				String name = parseKeyword("Domain", everything)[0];
				String[] resourceElements = parseKeyword("Resource", everything);
				HashMap<String,Integer> resources = processResources(resourceElements);
				String[] simpleOperators = parseKeyword("SimpleOperator", everything);
				String[] planningOperators = parseKeyword("PlanningOperator", everything);
				String[] sensors = parseKeyword("Sensor", everything);
				String[] actuators = parseKeyword("Actuator", everything);
				//String[] controllable = parseKeyword("Controllable", everything);

				String[] contextVars = parseKeyword("ContextVariable", everything);

                String[] timelinesString = parseKeyword("TimelinesToShow", everything);
                String[] timelines = null;
                if (timelinesString.length > 0) {
                    timelines =timelinesString[0].split("\\s");
                }

				int[] resourceCaps = new int[resources.keySet().size()];
				String[] resourceNames = new String[resources.keySet().size()];
				int resourceCounter = 0;
				for (String rname : resources.keySet()) {
					resourceNames[resourceCounter] = rname;
					resourceCaps[resourceCounter] = resources.get(rname);
					resourceCounter++;
				}

				System.out.println(domainType);
				SimpleDomain dom = null;
				if (domainType.equals(SimpleDomain.class)) {
					dom = new SimpleDomain(resourceCaps, resourceNames, name, everything);
				}
				else if (domainType.equals(FluentBasedSimpleDomain.class)) {
					dom = new FluentBasedSimpleDomain(resourceCaps, resourceNames, name, everything);
				}
				else if (domainType.equals(ProactivePlanningDomain.class)) {
					dom = new ProactivePlanningDomain(resourceCaps, resourceNames, name, everything);
				}

                ValueOrderingH valOH = new ValueOrderingH() {
                    @Override
                    public int compare(ConstraintNetwork arg0, ConstraintNetwork arg1) {
                        //Return unifications first
                        if (arg0.getAnnotation() != null && arg1.getAnnotation() != null) {
                            if (arg0.getAnnotation() instanceof Integer && arg1.getAnnotation() instanceof Integer) {
                                int annotation1 = ((Integer)arg0.getAnnotation()).intValue();
                                int annotation2 = ((Integer)arg1.getAnnotation()).intValue();
//                                System.out.println("............................ Returning " + (annotation2-annotation1));
                                return annotation2-annotation1;
                            }
                        }
                        //Return unifications first
                        //TODO: maybe this is superfluous...
                        return arg0.getVariables().length - arg1.getVariables().length;
                    }
                };

                //No variable ordering
                VariableOrderingH varOH = new VariableOrderingH() {
                    @Override
                    public int compare(ConstraintNetwork o1, ConstraintNetwork o2) { return 0; }
                    @Override
                    public void collectData(ConstraintNetwork[] allMetaVariables) { }
                };


                dom.setValOH(valOH);
                dom.setVarOH(varOH);
				
				for (String sensor : sensors) dom.addSensor(sensor);
				for (String act : actuators) dom.addActuator(act);
				for (String cv : contextVars) dom.addContextVar(cv);
				for (String operator : simpleOperators) {
					dom.addOperator(SimpleDomain.parseOperator(operator,resourceNames,false));
				}
				for (String operator : planningOperators) {
					dom.addOperator(SimpleDomain.parseOperator(operator,resourceNames,true));
				}
				//... and we also add all its resources as separate meta-constraints
				for (Schedulable sch : dom.getSchedulingMetaConstraints()) sp.addMetaConstraint(sch);

				//This adds the domain as a meta-constraint of the SimplePlanner
				sp.addMetaConstraint(dom);
                if (null != timelines) {
                    for (String timeline : timelines) dom.addTimeline(timeline);
                }


				return dom;
			}
			finally { br.close(); }
		}
		catch (FileNotFoundException e) { e.printStackTrace(); }
		catch (IOException e) { e.printStackTrace(); }
		return null;
	}

    private void addTimeline(String timeline) {
        this.timelines.add(timeline);
    }

}
