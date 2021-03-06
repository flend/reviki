package net.hillsdon.reviki.wiki.renderer.creole.ast;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;

import net.hillsdon.reviki.wiki.renderer.creole.CreoleASTBuilder;
import net.hillsdon.reviki.wiki.renderer.creole.CreoleRenderer;
import net.hillsdon.reviki.wiki.renderer.macro.Macro;

public class MacroNode extends TextNode implements BlockableNode<MacroNode> {

  private static final Log LOG = LogFactory.getLog(MacroNode.class);

  private final String _name;

  private final String _args;

  private final CreoleASTBuilder _visitor;

  public MacroNode(final String name, final String args, final CreoleASTBuilder visitor, final boolean isBlock) {
    super("<<" + name + ":" + args + ">>", true);
    _name = name;
    _args = args;
    _visitor = visitor;
    _isBlock = isBlock;
  }

  /** Create a new inline macro node. */
  public MacroNode(final String name, final String args, final CreoleASTBuilder visitor) {
    this(name, args, visitor, false);
  }

  @Override
  public List<ASTNode> expandMacrosInt(final Supplier<List<Macro>> macros) {
    // This is basically lifted from the old MacroNode.
    List<Macro> theMacros = macros.get();
    try {
      for (Macro macro : theMacros) {
        ASTNode out = null;

        if (macro.getName().equals(_name)) {
          String content = macro.handle(_visitor.page(), _args);
          switch (macro.getResultFormat()) {
            case XHTML:
              out = new Raw(content);
              break;
            case WIKI:
              out = CreoleRenderer.renderPartWithVisitor(content, _visitor, macros);
              break;
            default:
              out = new Plaintext(content);
          }
        }

        if (out != null) {
          if (out instanceof Page) {
            // Strip off the Page container.
            return out.getChildren();
          }
          else {
            return ImmutableList.of(out);
          }
        }
      }

    }
    catch (Exception e) {
      LOG.error("Error handling macro on: " + _visitor.page().getPath(), e);
    }

    // Failed to find a macro of the same name.
    return ImmutableList.of((ASTNode) this);
  }

  public MacroNode toBlock() {
    return new MacroNode(_name, _args, _visitor, true);
  }
}
