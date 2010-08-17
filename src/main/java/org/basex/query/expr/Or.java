package org.basex.query.expr;

import static org.basex.query.QueryText.*;
import static org.basex.query.QueryTokens.*;
import org.basex.query.IndexContext;
import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.item.Bln;
import org.basex.query.item.Item;
import org.basex.util.Array;
import org.basex.util.InputInfo;

/**
 * Or expression.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 */
public final class Or extends Logical {
  /**
   * Constructor.
   * @param ii input info
   * @param e expression list
   */
  public Or(final InputInfo ii, final Expr[] e) {
    super(ii, e);
  }

  @Override
  public Expr comp(final QueryContext ctx) throws QueryException {
    // remove atomic values
    final Expr c = super.comp(ctx);
    if(c != this) return c;

    // merge predicates if possible
    CmpG cmpg = null;
    Expr[] ex = {};
    for(final Expr e : expr) {
      Expr tmp = null;
      if(e instanceof CmpG) {
        // merge general comparisons
        final CmpG g = (CmpG) e;
        if(cmpg == null) cmpg = g;
        else if(cmpg.union(g, ctx)) tmp = g;
      }
      // no optimization found; add original expression
      if(tmp == null) ex = Array.add(ex, e);
    }
    if(ex.length != expr.length) ctx.compInfo(OPTWRITE, this);
    expr = ex;

    // return single expression if it yields a boolean
    return expr.length == 1 ? single() : this;
  }

  @Override
  public Item atomic(final QueryContext ctx, final InputInfo ii)
      throws QueryException {
    double d = 0;
    boolean f = false;
    for(final Expr e : expr) {
      final Item it = e.ebv(ctx, input);
      if(it.bool(input)) {
        final double s = it.score();
        if(s == 0) return Bln.TRUE;
        d = ctx.score.or(d, s);
        f = true;
      }
    }
    return d == 0 ? Bln.get(f) : Bln.get(d);
  }

  @Override
  public boolean indexAccessible(final IndexContext ic) throws QueryException {
    int is = 0;
    Expr[] exprs = {};
    for(final Expr e : expr) {
      if(!e.indexAccessible(ic) || ic.seq) return false;
      is += ic.is;
      // add only expressions that yield results
      if(ic.is != 0) exprs = Array.add(exprs, e);
    }
    ic.is = is;
    expr = exprs;
    return true;
  }

  @Override
  public Expr indexEquivalent(final IndexContext ic) throws QueryException {
    super.indexEquivalent(ic);
    return new Union(input, expr);
  }

  @Override
  public String toString() {
    return toString(" " + OR + " ");
  }
}
