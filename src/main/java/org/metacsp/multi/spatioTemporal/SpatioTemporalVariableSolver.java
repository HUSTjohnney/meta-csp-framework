package org.metacsp.multi.spatioTemporal;

import org.metacsp.framework.ConstraintSolver;
import org.metacsp.framework.multi.MultiConstraintSolver;
import org.metacsp.multi.allenInterval.AllenInterval;
import org.metacsp.multi.allenInterval.AllenIntervalConstraint;
import org.metacsp.multi.allenInterval.AllenIntervalNetworkSolver;
import org.metacsp.multi.spatial.DE9IM.DE9IMRelation;
import org.metacsp.multi.spatial.DE9IM.DE9IMRelationSolver;
import org.metacsp.multi.spatial.DE9IM.GeometricShapeVariable;

/**
 * A {@link MultiConstraintSolver} for {@link SpatioTemporalVariable}s. Constraints of type {@link AllenIntervalConstraint} and
 * {@link DE9IMRelation} can be added to {@link SpatioTemporalVariable}s.
 * 
 * @author Federico Pecora
 *
 */
public class SpatioTemporalVariableSolver extends MultiConstraintSolver {
	
	private static final long serialVersionUID = 5925181272826836649L;

	protected SpatioTemporalVariableSolver(Class<?>[] constraintTypes, Class<?> variableType, ConstraintSolver[] internalSolvers, int[] ingredients) {
		super(constraintTypes, variableType, internalSolvers, ingredients);
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Create a {@link SpatioTemporalVariableSolver} with given temporal origin and horizon.
	 * @param origin The origin of the temporal solver underlying this {@link SpatioTemporalVariableSolver}.
	 * @param horizon The horizon of the temporal solver underlying this {@link SpatioTemporalVariableSolver}.
	 */
	public SpatioTemporalVariableSolver(long origin, long horizon) {
		super(new Class[]{AllenIntervalConstraint.class,DE9IMRelation.class}, SpatioTemporalVariable.class, createInternalConstraintSolvers(origin, horizon), new int[]{1,1});
	}
	
	private static ConstraintSolver[] createInternalConstraintSolvers(long origin, long horizon) {
		ConstraintSolver[] ret = new ConstraintSolver[2];
		ret[0] = new AllenIntervalNetworkSolver(origin, horizon);
		ret[1] = new DE9IMRelationSolver();
		return ret;
	}
	
	@Override
	public boolean propagate() {
		return true;
	}
	
	/**
	 * Returns the {@link AllenIntervalNetworkSolver} which propagates temporal constraints (of type {@link AllenIntervalConstraint})
	 * among the temporal parts ({@link AllenInterval}s) of {@link SpatioTemporalVariable}s.
	 * @return The temporal solver responsible for propagating the temporal constraints among
	 * the temporal parts of {@link SpatioTemporalVariable}s.
	 */
	public AllenIntervalNetworkSolver getTemporalSolver() {
		return (AllenIntervalNetworkSolver)this.getConstraintSolvers()[0];
	}

	/**
	 * Returns the {@link DE9IMRelationSolver} which propagates spatial constraints (of type {@link DE9IMRelation})
	 * among the spatial parts ({@link GeometricShapeVariable}s) of {@link SpatioTemporalVariable}s.
	 * @return The spatial solver responsible for propagating the spatial constraints among
	 * the spatial parts of {@link SpatioTemporalVariable}s.
	 */
	public DE9IMRelationSolver getSpatialSolver() {
		return (DE9IMRelationSolver)this.getConstraintSolvers()[1];
	}

}
