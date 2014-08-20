package net.hillsdon.reviki.wiki.renderer.creole.ast;

import java.util.List;

public class Page extends ASTNode {
  public Page(final List<ASTNode> blocks) {
    super(blocks);
  }
  
  @Override
  protected void toSmallString(StringBuilder sb) {
    for (ASTNode child : getChildren()) {
      child.toSmallString(sb);
    }
  }
}
