// Example: HelloKolaUI.java
import kola.main.BaseWindow;
import kola.components.Button;

public class HelloUI {
public static void main(String[] args) {
BaseWindow window = new BaseWindow("Hello UI", 400, 300);
Button btn = new Button("Click me", () -> System.out.println("Clicked!"));
window.setContent(btn);
window.show();
}
}

// Example: Custom Component
import kola.main.BaseComp;

public class MyBadge extends BaseComp {
public MyBadge(String text) {
// ...
}
@Override
public void render(java.awt.Graphics2D g) {
// ...
}
}

// Example: Styling
Div card = new Div();
card.setStyle("bg-white rounded-[12] shadow p-4 w-[320] h-[180]");

// Example: ResizableDiv
ResizableDiv resDiv = new ResizableDiv();
resDiv.setContent(new Label("Drag to resize me!"));

// Example: SVG
SvgFromStringComp svg = new SvgFromStringComp("<svg viewBox='0 0 100 100'><circle cx='50' cy='50' r='40' fill='red'/></svg>");

// Example: Image
ImageComp img = new ImageComp("https://example.com/image.png", "Alt text");

// Example: Modal
ConfirmDialog dialog = new ConfirmDialog("Are you sure?", () -> onYes(), () -> onNo());

// Example: Game Loop (see apps.snake.SnakeGameApp)

// See README.md for more details and usage patterns.
