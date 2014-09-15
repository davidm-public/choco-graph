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

package solver.cstrs.basic;

import solver.constraints.Propagator;
import solver.constraints.PropagatorPriority;
import solver.exception.ContradictionException;
import solver.variables.GraphEventType;
import solver.variables.IGraphVar;
import solver.variables.IntVar;
import solver.variables.Variable;
import solver.variables.events.IntEventType;
import util.ESat;
import util.objects.setDataStructures.ISet;

/**
 * Propagator that ensures that Nb arcs/edges belong to the final graph
 *
 * @author Jean-Guillaume Fages
 */
public class PropNbArcs extends Propagator {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    protected IGraphVar g;
    protected IntVar k;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    public PropNbArcs(IGraphVar graph, IntVar k) {
        super(new Variable[]{graph, k}, PropagatorPriority.LINEAR, false);
        this.g = graph;
        this.k = k;
    }

    //***********************************************************************************
    // PROPAGATIONS
    //***********************************************************************************

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        int nbK = 0;
        int nbE = 0;
        ISet env = g.getPotentialNodes();
        for (int i = env.getFirstElement(); i >= 0; i = env.getNextElement()) {
            nbE += g.getPotSuccOrNeighOf(i).getSize();
            nbK += g.getMandSuccOrNeighOf(i).getSize();
        }
        if (!g.isDirected()) {
            nbK /= 2;
            nbE /= 2;
        }
        filter(nbK, nbE);
    }

    private void filter(int nbK, int nbE) throws ContradictionException {
        k.updateLowerBound(nbK, aCause);
        k.updateUpperBound(nbE, aCause);
        if (nbK != nbE && k.isInstantiated()) {
            ISet nei;
            ISet env = g.getPotentialNodes();
            if (k.getValue() == nbE) {
                for (int i = env.getFirstElement(); i >= 0; i = env.getNextElement()) {
                    nei = g.getUB().getSuccOrNeighOf(i);
                    for (int j = nei.getFirstElement(); j >= 0; j = nei.getNextElement()) {
                        g.enforceArc(i, j, aCause);
                    }
                }
            }
            if (k.getValue() == nbK) {
                ISet neiKer;
                for (int i = env.getFirstElement(); i >= 0; i = env.getNextElement()) {
                    nei = g.getUB().getSuccOrNeighOf(i);
                    neiKer = g.getLB().getSuccOrNeighOf(i);
                    for (int j = nei.getFirstElement(); j >= 0; j = nei.getNextElement()) {
                        if (!neiKer.contain(j)) {
                            g.removeArc(i, j, aCause);
                        }
                    }
                }
            }
        }
    }

    //***********************************************************************************
    // INFO
    //***********************************************************************************

    @Override
    public int getPropagationConditions(int vIdx) {
        return GraphEventType.REMOVE_ARC.getMask() + GraphEventType.ADD_ARC.getMask()
                + IntEventType.INCLOW.getMask() + IntEventType.DECUPP.getMask() + IntEventType.INSTANTIATE.getMask();
    }

    @Override
    public ESat isEntailed() {
        int nbK = 0;
        int nbE = 0;
        ISet env = g.getPotentialNodes();
        for (int i = env.getFirstElement(); i >= 0; i = env.getNextElement()) {
            nbE += g.getUB().getSuccOrNeighOf(i).getSize();
            nbK += g.getLB().getSuccOrNeighOf(i).getSize();
        }
        if (!g.isDirected()) {
            nbK /= 2;
            nbE /= 2;
        }
        if (nbK > k.getUB() || nbE < k.getLB()) {
            return ESat.FALSE;
        }
        if (k.isInstantiated() && g.isInstantiated()) {
            return ESat.TRUE;
        }
        return ESat.UNDEFINED;
    }
}
