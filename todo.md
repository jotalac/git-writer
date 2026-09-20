## TODO

> high priority

- add tests :((((
- add encryption possibility

> medium priority

- add something like .gitwriter file to each notebook folder, there will be the remote credentials etc., it wont be commited, but comes in handy when you want to open folder with remote already configured 
- when opening notebooks, when two noteboks have same directory name but the path is different, it cannot be opened
- add bottom bar - info button and character/word count (add some help to topAppBar - usage, shortcuts in the app,
  markdown cheatsheet, etc.)
- add ctrl+r replace (replace all/current)
- mobile keyboard keeps the editor shifted even after keyboard hide when new block is created via 'enter' press (android
  17 issue only now - probably a Android 17 bug and will be fixed later - ime freezes)
- enable proguad and manually configure it to lower the binary size (android and desktop)
- follow clean architecture and introduce UseCases instead of injecting repositories into *r*epositories
- run periodic git fetch to display if the notes are actually up to date (or just run it once on notebook open)
- made custom top app bar - it looks horrible on Windows at least
- add spell checks for current active block
- in the settings as possiblity to change the commit message

> other ideas - lower priority

- share notes on mobile? like normal share button to quickshare, email ...
- in the file tree show which files are modified (based on the git status)
- when renaming image resource – refactor the notes to use the new image name?
- add some highlight cursor to the file tree so that i can be operated only with keyboard (f2 for renaming and ctrl+n
  for new file)
- better messages while syncing (eg. when nothing is commited or pulled - display up to date message)
- make username and password optional for cloning when cloning public repo – then the sync would be disabled?
