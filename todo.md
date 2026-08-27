## TODO

> high priority

- when no notebook is created and opened - git-sync status is up-to-date
- mobile keyboard sometimes keep the editor shifted even after keyboard hide

> medium priority

- enable proguad and manually configure it (android and desktop)
- make ctrl+n create new note
- right click on filetree background should also bring up the context menu with create file and create folder options
- follow clean architecture and introduce UseCases instead of injecting repositories into repositories
- run periodic git fetch to display if the notes are actually up to date (or just run it once on notebook open)
- made custom top app bar - it looks horrible on Windows at least

> other ideas - low priority

- default warning, tip, success github like quotes that should be supported by mikepenz markdown library - doesnt work
- when renaming image resource – refactor the notes to use the new image name?
- maybe add the action bar on desktop - like it is in the IDEA markdown editor
- add rounded corners to rendered image in the editor
- add some highlight cursor to the file tree so that i can be operated only with keyboard (f2 for ranaming and ctrl+n
  for new file)
- encrypt data before pushing to remote
- better messages while syncing (eg. when nothing is commited or pulled - dispaly up to date message)

- make username and password optional for cloning when cloning public repo – then the sync would be disabled?
