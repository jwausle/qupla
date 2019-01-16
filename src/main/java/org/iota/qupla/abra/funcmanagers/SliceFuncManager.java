package org.iota.qupla.abra.funcmanagers;

import org.iota.qupla.abra.AbraModule;
import org.iota.qupla.abra.block.AbraBlockBranch;
import org.iota.qupla.abra.block.base.AbraBaseBlock;
import org.iota.qupla.abra.block.site.AbraSiteMerge;
import org.iota.qupla.abra.block.site.AbraSiteParam;
import org.iota.qupla.abra.funcmanagers.base.BaseFuncManager;

public class SliceFuncManager extends BaseFuncManager
{
  private int start;

  public SliceFuncManager()
  {
    super("slice");
  }

  @Override
  protected void createInstance()
  {
    super.createInstance();

    if (start > 0)
    {
      branch.addInputParam(start);
    }

    final AbraSiteParam inputSite = branch.addInputParam(size);

    final AbraSiteMerge merge = new AbraSiteMerge();
    merge.size = size;
    merge.inputs.add(inputSite);
    inputSite.references++;

    branch.specialType = AbraBaseBlock.TYPE_SLICE;
    branch.offset = start;
    branch.outputs.add(merge);
  }

  public AbraBlockBranch find(final AbraModule module, final int size, final int start)
  {
    this.module = module;
    this.size = size;
    this.start = start;
    name = funcName + SEPARATOR + size + SEPARATOR + start;
    return findInstance();
  }
}
