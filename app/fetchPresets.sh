#!/bin/bash

function deletePresetsRepo {
    rm -rf vocable-presets
}

# Ensure that we start fresh by deleting any existing vocable-presets repo
deletePresetsRepo

# Clone the vocable-presets repo. We will be copying files over from this repo.
git clone git@github.com:willowtreeapps/vocable-presets.git

if [ $? -ne 0 ]; then
    echo "fetchPresets.sh: Error: Failed to clone vocable-presets repo"
    exit 1
fi

# Copy presets.json and presets.xml files over
cp vocable-presets/presets.json src/main/assets/json
cp -r vocable-presets/android/* src/main/res

deletePresetsRepo

echo "fetchPresets.sh: Successfully fetched presets"
