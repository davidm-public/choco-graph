/**
 *  Copyright (c) 1999-2014, Ecole des Mines de Nantes
 *  All rights reserved.
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *      * Neither the name of the Ecole des Mines de Nantes nor the
 *        names of its contributors may be used to endorse or promote products
 *        derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.chocosolver.graphsolver.cstrs.channeling.edges;

import org.chocosolver.graphsolver.variables.GraphEventType;
import org.chocosolver.graphsolver.variables.GraphVar;
import org.chocosolver.graphsolver.variables.IncidentSet;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.tools.ArrayUtils;

/**
 * @author Jean-Guillaume Fages
 */
public class PropNeighBoolChannel extends Propagator<Variable> {

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private BoolVar[] bools;
	private GraphVar g;
	private int vertex;
	private IncidentSet inc;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropNeighBoolChannel(BoolVar[] neigh, final int vertex, GraphVar gV, IncidentSet incSet) {
		super(ArrayUtils.append(neigh,new Variable[]{gV}), PropagatorPriority.LINEAR, true);
		this.vertex = vertex;
		this.bools = neigh;
		this.g = gV;
		this.inc = incSet;
		super.linkVariables();
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public int getPropagationConditions(int vIdx) {
		if (vIdx == bools.length) {
			return GraphEventType.ADD_ARC.getMask() + GraphEventType.REMOVE_ARC.getMask();
		}else{
			return IntEventType.all();
		}
	}

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		for(int i: inc.getPotSet(g,vertex)){
			if(bools[i].getUB()==0){
				inc.remove(g,vertex,i,this);
			}else if(bools[i].getLB()==1){
				inc.enforce(g,vertex,i,this);
			}
		}
		for(int i=0;i<bools.length;i++){
			if(!inc.getPotSet(g,vertex).contains(i)){
				bools[i].setToFalse(this);
			}else if(inc.getMandSet(g,vertex).contains(i)){
				bools[i].setToTrue(this);
			}
		}
	}

	@Override
	public void propagate(int idxVarInProp, int mask) throws ContradictionException {
		if (idxVarInProp < bools.length) {
			if(bools[idxVarInProp].getLB()==1){
				inc.enforce(g,vertex,idxVarInProp,this);
			}else{
				inc.remove(g,vertex,idxVarInProp,this);
			}
		} else {
			for(int i=0;i<bools.length;i++){
				if(!inc.getPotSet(g,vertex).contains(i)){
					bools[i].setToFalse(this);
				}else if(inc.getMandSet(g,vertex).contains(i)){
					bools[i].setToTrue(this);
				}
			}
		}
	}

	@Override
	public ESat isEntailed() {
		for(int i=0;i<bools.length;i++){
			if(bools[i].getLB()==1 && !inc.getPotSet(g, vertex).contains(i)){
				return ESat.FALSE;
			}
		}
		for(int i:inc.getMandSet(g, vertex)){
			if(bools[i].getUB()==0){
				return ESat.FALSE;
			}
		}
		if(isCompletelyInstantiated()){
			return ESat.TRUE;
		}
		return ESat.UNDEFINED;
	}
}
