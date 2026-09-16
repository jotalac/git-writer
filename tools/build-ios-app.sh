# 1. Build the .app for physical iOS hardware without code signing
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Release \
  -destination 'generic/platform=iOS' \
  -derivedDataPath build/ios_release \
  CODE_SIGNING_ALLOWED=NO \
  CODE_SIGNING_REQUIRED=NO

# 2. Package into an .ipa using the Payload structure
rm -rf build/Payload build/git-writer.ipa
mkdir -p build/Payload
cp -r build/ios_release/Build/Products/Release-iphoneos/*.app build/Payload/
cd build && zip -qr git-writer.ipa Payload
rm -rf Payload ios_release