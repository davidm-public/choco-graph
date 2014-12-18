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

package solver.search.strategy;

import solver.search.GraphAssignment;
import solver.search.strategy.decision.Decision;
import solver.search.GraphDecision;
import solver.search.strategy.arcs.LexArc;
import solver.search.strategy.nodes.LexNode;
import solver.search.strategy.strategy.AbstractStrategy;
import solver.variables.IGraphVar;
import util.PoolManager;

/**
 * <br/>
 *
 * @author Jean-Guillaume Fages
 * @since 1 April 2011
 */
public class GraphStrategy extends AbstractStrategy<IGraphVar> {

    protected IGraphVar g;
    protected NodeStrategy nodeStrategy;
    protected ArcStrategy arcStrategy;
    protected NodeArcPriority priority;
    protected PoolManager<GraphDecision> pool;

    public enum NodeArcPriority {
        NODES_THEN_ARCS,
        ARCS;
    }

    public GraphStrategy(IGraphVar g, NodeStrategy ns, ArcStrategy as, NodeArcPriority priority) {
        super(new IGraphVar[]{g});
        this.g = g;
        this.nodeStrategy = ns;
        this.arcStrategy = as;
        this.priority = priority;
        pool = new PoolManager<>();
    }

    public GraphStrategy(IGraphVar g) {
        this(g, new LexNode(g), new LexArc(g), NodeArcPriority.NODES_THEN_ARCS);
    }

    @Override
    public void init() {
    }

    @Override
    public GraphDecision getDecision() {
        if (g.isInstantiated()) {
            return null;
        }
        GraphDecision dec = pool.getE();
        if (dec == null) {
            dec = new GraphDecision(pool);
        }
        switch (priority) {
            case NODES_THEN_ARCS:
                int node = nextNode();
                if (node != -1) {
                    dec.setNode(g, node, GraphAssignment.graph_enforcer);
                } else {
					if(arcStrategy==null){
						return null;
					}
                    nextArc();
                    dec.setArc(g, arcStrategy.getFrom(), arcStrategy.getTo(), GraphAssignment.graph_enforcer);
                }
                break;
            case ARCS:
            default:
                if(!nextArc()){
					return null;
				}
                dec.setArc(g, arcStrategy.getFrom(), arcStrategy.getTo(), GraphAssignment.graph_enforcer);
                break;
        }
        return dec;
    }

    public int nextNode() {
        return nodeStrategy.nextNode();
    }

    public boolean nextArc() {
        return arcStrategy.computeNextArc();
    }
}
