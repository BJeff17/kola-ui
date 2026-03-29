sed -i '/private final BaseComp content;/a \        private final ScrollView mainScroll;' src/Main.java
sed -i 's/this.content = window.getContent();/this.content = window.getContent();\n            this.mainScroll = new ScrollView(0, 0, content.getWidth(), content.getHeight());\n            this.content.addChild(mainScroll);/g' src/Main.java
