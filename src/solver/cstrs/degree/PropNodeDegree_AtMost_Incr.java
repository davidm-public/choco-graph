/*
 * Copyright (c) 1999-2014, Ecole des Mines de Nantes
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Ecole des Mines de Nantes nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package solver.cstrs.degree;

import solver.constraints.Propagator;
import solver.constraints.PropagatorPriority;
import solver.exception.ContradictionException;
import solver.variables.*;
import solver.variables.delta.IGraphDeltaMonitor;
import util.ESat;
import util.objects.graphs.Orientation;
import util.objects.setDataStructures.ISet;
import util.procedure.PairProcedure;

/**
 * Propagator that ensures that a node has at most N successors/predecessors/neighbors
 *
 * @author Jean-Guillaume Fages
 */
public class PropNodeDegree_AtMost_Incr extends Propagator<IGraphVar> {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    private IGraphVar g;
    private IGraphDeltaMonitor gdm;
    private PairProcedure enf_proc;
    private int[] degrees;
    private IncidentSet target;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    public PropNodeDegree_AtMost_Incr(IDirectedGraphVar graph, Orientation setType, int degree) {
        this(graph, setType, buildArray(degree, graph.getNbMaxNodes()));
    }

    public PropNodeDegree_AtMost_Incr(IDirectedGraphVar graph, Orientation setType, int[] degrees) {
        super(new IDirectedGraphVar[]{graph}, PropagatorPriority.BINARY, true);
        g = graph;
        gdm = g.monitorDelta(this);
        this.degrees = degrees;
        switch (setType) {
            case SUCCESSORS:
                target = new IncidentSet.SuccOrNeighSet();
                enf_proc = new PairProcedure() {
                    public void execute(int i, int j) throws ContradictionException {
                        checkAtMost(i);
                    }
                };
                break;
            case PREDECESSORS:
                target = new IncidentSet.PredOrNeighSet();
                enf_proc = new PairProcedure() {
                    public void execute(int i, int j) throws ContradictionException {
                        checkAtMost(j);
                    }
                };
                break;
            default:
                throw new UnsupportedOperationException("wrong parameter: use either PREDECESSORS or SUCCESSORS");
        }
    }

    public PropNodeDegree_AtMost_Incr(IUndirectedGraphVar graph, int degree) {
        this(graph, buildArray(degree, graph.getNbMaxNodes()));
    }

    public PropNodeDegree_AtMost_Incr(final IUndirectedGraphVar graph, int[] degrees) {
        super(new IUndirectedGraphVar[]{graph}, PropagatorPriority.BINARY, true);
        target = new IncidentSet.SuccOrNeighSet();
        g = graph;
        gdm = g.monitorDelta(this);
        this.degrees = degrees;
        enf_proc = new PairProcedure() {
            public void execute(int i, int j) throws ContradictionException {
                checkAtMost(i);
                checkAtMost(j);
            }
        };
    }

    private static int[] buildArray(int degree, int n) {
        int[] degrees = new int[n];
        for (int i = 0; i < n; i++) {
            degrees[i] = degree;
        }
        return degrees;
    }

    //***********************************************************************************
    // PROPAGATIONS
    //***********************************************************************************

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        ISet act = g.getPotentialNodes();
        for (int node = act.getFirstElement(); node >= 0; node = act.getNextElement()) {
            checkAtMost(node);
        }
        gdm.unfreeze();
    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
        gdm.freeze();
        gdm.forEachArc(enf_proc, GraphEventType.ADD_ARC);
        gdm.unfreeze();
    }

    //***********************************************************************************
    // INFO
    //***********************************************************************************

    @Override
    public int getPropagationConditions(int vIdx) {
        return GraphEventType.ADD_ARC.getMask();
    }

    @Override
    public ESat isEntailed() {
        ISet act = g.getMandatoryNodes();
        for (int i = act.getFirstElement(); i >= 0; i = act.getNextElement()) {
            if (target.getPotSet(g, i).getSize() > degrees[i]) {
                return ESat.FALSE;
            }
        }
        if (!g.isInstantiated()) {
            return ESat.UNDEFINED;
        }
        return ESat.TRUE;
    }

    //***********************************************************************************
    // PROCEDURES
    //***********************************************************************************

    /**
     * When a node has more than N successors/predecessors/neighbors then it must be removed,
	 * (which results in a failure)
     * If it has N successors/predecessors/neighbors in the kernel then other incident edges
     * should be removed
     */
    private void checkAtMost(int i) throws ContradictionException {
        ISet ker = target.getMandSet(g, i);
        ISet env = target.getPotSet(g, i);
        int size = ker.getSize();
        if (size > degrees[i]) {
            g.removeNode(i, aCause);
        } else if (size == degrees[i] && env.getSize() > size) {
            for (int other = env.getFirstElement(); other >= 0; other = env.getNextElement()) {
                if (!ker.contain(other)) {
                    target.remove(g, i, other, aCause);
                }
            }
        }
    }
}
