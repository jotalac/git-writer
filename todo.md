## TODO

> high priority

- mobile keyboard keeps the editor shifted even after keyboard hide when new block is created via 'enter' press (works
  on older android 12 for some reason)

> medium priority

- launcher icon doenst work on linux (tested only on fedora via .rpm and appImage)
- enable proguad and manually configure it to lower the binary size (android and desktop)
- make ctrl+n create new note
- right click on filetree background should also bring up the context menu with create file and create folder options
- follow clean architecture and introduce UseCases instead of injecting repositories into repositories
- run periodic git fetch to display if the notes are actually up to date (or just run it once on notebook open)
- made custom top app bar - it looks horrible on Windows at least

> other ideas - lower priority

- in the file tree show which files are modified (based on the git status)
- when renaming image resource – refactor the notes to use the new image name?
- maybe add the action bar on desktop - like it is in the IDEA markdown editor
- add rounded corners to rendered image in the editor
- add some highlight cursor to the file tree so that i can be operated only with keyboard (f2 for ranaming and ctrl+n
  for new file)
- encrypt data before pushing to remote
- better messages while syncing (eg. when nothing is commited or pulled - display up to date message)

- make username and password optional for cloning when cloning public repo – then the sync would be disabled?
