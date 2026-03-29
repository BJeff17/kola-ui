# WorkSuite Pro (Complete App)

Cette application est une vraie démo complète construite sur votre moteur UI:

- Menu bar multi-vues (`Dashboard`, `Journal de Notes`, `Journal de Bord`, `Paramètres`)
- Modal fonctionnelle d'ajout pour tâches, notes, logs
- Dashboard projet (liste de tâches, statut, suppression)
- Journal de Notes (création, édition, suppression)
- Journal de Bord (historique des événements, ajout manuel, purge)
- Paramètres (édition, sauvegarde, aide runtime scroll)
- Panneau latéral de pilotage avec stats en direct

## Fichiers principaux

- `src/apps/worksuite/WorkSuiteApp.java`
- `src/components/NavMenuBar.java`

## Compilation

```bash
javac -d output $(find src -name '*.java')
```

## Lancement

```bash
java -cp output apps.worksuite.WorkSuiteApp 0
```

## Lancement avec tuning scroll

```bash
java -Dui.scroll.xFactor=1.2 -Dui.scroll.yFactor=0.9 -Dui.scroll.latchMs=260 -cp output apps.worksuite.WorkSuiteApp 0
```

## Notes

- `0` en argument final active le mode passif (repaint optimisé par invalidation).
- Vous pouvez mettre un FPS > 0 pour un rendu actif continu.
