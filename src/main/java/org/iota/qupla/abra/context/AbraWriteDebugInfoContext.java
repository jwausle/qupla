package org.iota.qupla.abra.context;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.AbraBlockImport;
import org.iota.qupla.abra.block.AbraBlockLut;
import org.iota.qupla.abra.block.site.AbraSiteKnot;
import org.iota.qupla.abra.block.site.AbraSiteLatch;
import org.iota.qupla.abra.block.site.AbraSiteMerge;
import org.iota.qupla.abra.block.site.AbraSiteParam;
import org.iota.qupla.abra.block.site.base.AbraBaseSite;
import org.iota.qupla.abra.context.base.AbraTritCodeBaseContext;
import org.iota.qupla.qupla.expression.base.BaseExpr;

public class AbraWriteDebugInfoContext extends AbraTritCodeBaseContext
{
  @Override
  public void eval(final AbraModule module)
  {
    //TODO add all types to context and write them out
    //     (vector AND struct) name/size/isFloat?

    evalBlocks(module.luts);
    evalBlocks(module.branches);
    evalBlocks(module.imports);

    //TODO generate tritcode hash so that we can create a function
    //     in the normal tritcode that returns that hash as a constant
  }

  @Override
  public void evalBranch(final AbraBlockBranch branch)
  {
    putString(branch.name);
    putString(branch.origin == null ? null : branch.origin.toString());
    evalBranchSites(branch);
  }

  @Override
  public void evalImport(final AbraBlockImport imp)
  {
    //TODO origin?
    putString(imp.name);
  }

  @Override
  public void evalKnot(final AbraSiteKnot knot)
  {
    evalSite(knot);
  }

  @Override
  public void evalLatch(final AbraSiteLatch latch)
  {
    evalSite(latch);
  }

  @Override
  public void evalLut(final AbraBlockLut lut)
  {
    final boolean isUnnamed = lut.name.equals(AbraBlockLut.unnamed(lut.lookup));
    putString(isUnnamed ? null : lut.name);
    putString(lut.origin == null ? null : lut.origin.toString());
  }

  @Override
  public void evalMerge(final AbraSiteMerge merge)
  {
    evalSite(merge);
  }

  @Override
  public void evalParam(final AbraSiteParam param)
  {
    evalSite(param);
  }

  private void evalSite(final AbraBaseSite site)
  {
    //TODO putInt(typeId) (index of origin.typeInfo)
    putString(site.name);
    for (BaseExpr stmt = site.stmt; stmt != null; stmt = stmt.next)
    {
      putString(stmt.toString());
    }
    putString(null);

  }
}
