## TODO

> high priority

- add multiple tabs on the top action bar

> medium priority

- share notes on mobile? like normal share button to quickshare, email ...
- create release for iOS and macOS
- encrypt data before pushing to remote
- make code block content line wrap - now it just overflows
- mobile keyboard keeps the editor shifted even after keyboard hide when new block is created via 'enter' press (android
  17 issue only now - probably a Android 17 bug and will be fixed later - ime freezes)
- enable proguad and manually configure it to lower the binary size (android and desktop)
- add some help to topAppBar - usage, shortcuts in the app, markdown cheatsheet, etc.
- make ctrl+n create new note
- follow clean architecture and introduce UseCases instead of injecting repositories into *r*epositories
- run periodic git fetch to display if the notes are actually up to date (or just run it once on notebook open)
- made custom top app bar - it looks horrible on Windows at least
- add spell checks for current active block

> other ideas - lower priority

- in the file tree show which files are modified (based on the git status)
- when renaming image resource – refactor the notes to use the new image name?
- maybe add the action bar on desktop - like it is in the IDEA markdown editor
- add some highlight cursor to the file tree so that i can be operated only with keyboard (f2 for renaming and ctrl+n
  for new file)
- better messages while syncing (eg. when nothing is commited or pulled - display up to date message)
- make username and password optional for cloning when cloning public repo – then the sync would be disabled?
- add tests :((((