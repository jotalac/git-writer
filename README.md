# Git-writer - git synced markdown editor

## TODO

> high priority

- move the ctrl + z and ctrl + y to the markdown editor and not the active block (on mobile it will stay in the toolbar)
- pressing enter on the keyboard will actually confirm the alert dialogs

> medium priority

- right click on filetree background should also bring up the context menu with create file and create folder options
- follow clean architecture and introduce UseCases instead of injecting repositories into repositories
- in the app settings there will be option to set the conflict resolution strategy - auto merge (always local/remote) or
  always manual solve

> other ideas - low priority

- default warning, tip, success github like quotes that should be supported by mikepenz markdown library - doesnt work
- when renaming image resource – refactor the notes to use the new image name?
- maybe add the action bar on desktop - like it is in the IDEA markdown editor
- add rounded corners to rendered image in the editor
- maybe run periodic git fetch to display if the notes are actually up to date (or just run it once on notebook open)
- add some highlight cursor to the file tree so that i can be operated only with keyboard (f2 for ranaming and ctrl+n
  for new file)

- make username and password optional for cloning when cloning public repo – then the sync would be disabled?


- ![img.png](img.png)
- ![img_1.png](img_1.png)