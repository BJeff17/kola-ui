sed -i 's/private final BaseLayoutEngine layoutEngine;/private BaseLayoutEngine layoutEngine;/g' src/style/StyleManager.java
sed -i '/public void setGridProps/a \    public void setLayoutEngineType(String display) {\n        this.layoutEngine = createLayoutEngine(display);\n    }' src/style/StyleManager.java
sed -i '/public StyleManager/a \    public StyleManager(String tailwindClasses) {\n        this.color = new Color(0,0,0,0);\n        this.layoutEngine = new layout.BlockLayoutEngine();\n        TailwindParser.applyTailwind(this, tailwindClasses);\n    }' src/style/StyleManager.java
