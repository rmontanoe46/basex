package org.basex.query.expr;

import static org.basex.query.QueryTokens.*;
import java.io.IOException;
import org.basex.core.Main;
import org.basex.data.Serializer;
import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.item.Item;
import org.basex.query.item.SeqType;
import org.basex.query.item.Type;
import org.basex.util.InputInfo;
import org.basex.util.Token;

/**
 * Cast expression.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 */
public final class Cast extends Single {
  /**
   * Function constructor.
   * @param ii input info
   * @param e expression
   * @param t data type
   */
  public Cast(final InputInfo ii, final Expr e, final SeqType t) {
    super(ii, e);
    type = t;
  }

  @Override
  public Expr comp(final QueryContext ctx) throws QueryException {
    checkUp(expr, ctx);
    super.comp(ctx);

    Expr e = this;
    if(expr.value()) {
      // pre-evaluate value
      e = preEval(ctx);
    } else if(type.type == Type.BLN || type.type == Type.FLT ||
        type.type == Type.DBL || type.type == Type.QNM ||
        type.type == Type.URI) {
      // skip cast if specified and return types are equal
      final SeqType t = expr.type();
      if(t.eq(type)) e = expr;
      else if(t.type == type.type && t.one() && type.zeroOrOne()) e = expr;
      if(e != this) optPre(e, ctx);
    }
    return e;
  }

  @Override
  public Item atomic(final QueryContext ctx, final InputInfo ii)
      throws QueryException {
    return type.cast(expr.atomic(ctx, input), this, ctx, input);
  }

  @Override
  public void plan(final Serializer ser) throws IOException {
    ser.openElement(this, TYPE, Token.token(type.toString()));
    expr.plan(ser);
    ser.closeElement();
  }

  @Override
  public String desc() {
    return type.type + " " + CAST;
  }

  @Override
  public String toString() {
    return Main.info("% cast as %", expr, type);
  }
}
