## TODO

> high priority

- create release for iOS and macOS
- notarize and sign the app for macos (or add guide in the readme how to run it without the signing)
- open notebook from folder (in the create notebook dialog there will be option to open notebook from folder)
- when moving notes to another folder in the file tree, it starts to edit the name automatically (bad logic in the viewmodel for starting editing)
- change the appereance of the ordered list - make the numbers gray and subnumbers differently counted

> medium priority

- add bottom bar - info button and character/word count (add some help to topAppBar - usage, shortcuts in the app,
  markdown cheatsheet, etc.)
- encrypt data before pushing to remote
- add ctrl+r replace (replace all/current)
- mobile keyboard keeps the editor shifted even after keyboard hide when new block is created via 'enter' press (android
  17 issue only now - probably a Android 17 bug and will be fixed later - ime freezes)
- enable proguad and manually configure it to lower the binary size (android and desktop)
- follow clean architecture and introduce UseCases instead of injecting repositories into *r*epositories
- run periodic git fetch to display if the notes are actually up to date (or just run it once on notebook open)
- made custom top app bar - it looks horrible on Windows at least
- add spell checks for current active block
  
- add tests :((((

> other ideas - lower priority

- share notes on mobile? like normal share button to quickshare, email ...
- in the file tree show which files are modified (based on the git status)
- when renaming image resource – refactor the notes to use the new image name?
- add some highlight cursor to the file tree so that i can be operated only with keyboard (f2 for renaming and ctrl+n
  for new file)
- better messages while syncing (eg. when nothing is commited or pulled - display up to date message)
- make username and password optional for cloning when cloning public repo – then the sync would be disabled?
