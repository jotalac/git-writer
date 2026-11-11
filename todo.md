## TODO

> high priority

- context menu on words with spellchecks are appearing very off
- support users custom certificates for desktop (Android and iOS already works)
- better padding to the code blocks + border

> medium priority

- add something like quick note, which it be deleted after the application exit (for some quick test notes)
- add note linking
- save the expanded folder to database (so that the file tree looks the same on app open)+ save all tabs that were opened
- add bottom bar - info button and character/word count (add some help to topAppBar - usage, shortcuts in the app,
  markdown cheatsheet, etc.)
- add ctrl+r replace (replace all/current)
- follow clean architecture and introduce UseCases instead of injecting repositories into *r*epositories
- run periodic git fetch to display if the notes are actually up to date (or just run it once on notebook open)
- in the settings as possibility to change the commit message
- add nucleus fs-watcher (or other fs watcher)
- fix pdf export -> dont export the compose page -> convert to html for proper text selection and reduced size
- store the repository access tokens and the encryption derived keys in a encrypted store on device eg: Android KeyStore, EncryptedSharedPreferences, iOS/macOS keychain, windows credentials manager, linux secret service
- make section collapsible, like in obsidian, eg. everything under h1 can be collased

> other ideas - lower priority  

- add support for mermaid diagrams - probably not
- make file header like in obsidian - the file heading will show the file name changing the header will change the filename - probably not
- when there is a merge conflict, add preview on the conflicted files and content
- when note is opened and image is clicked from the sidebar, make the image appear on new tab
- share notes on mobile? like normal share button to quickshare, email ...
- in the file tree show which files are modified (based on the git status)
- when renaming image resource – refactor the notes to use the new image name?
- add some highlight cursor to the file tree so that i can be operated only with keyboard (f2 for renaming and ctrl+n
  for new file)
- make username and password optional for cloning when cloning public repo – then the sync would be disabled?
- add some overview of the opened note, it will show the headers, and you can jump to the specified header
