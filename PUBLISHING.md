# 🚀 Automated Publishing Guide

Publish your MealPlanner app to Google Play Console automatically using Python.

## Setup Instructions

### Step 1: Create Service Account in Google Play Console

1. Go to [Google Play Console](https://play.google.com/console)
2. Select your project (mealplannerdb-510ff)
3. Navigate to **Settings** → **API access**
4. Under "Service Accounts", click **+ Create service account**
5. Click the new service account name
6. Under "Keys", click **+ Create key** → **JSON**
7. Save the JSON file as `play-console-credentials.json` in the **project root** (same folder as `publish.py`)

### Step 2: Grant Service Account Permissions

1. Stay in Play Console → **Settings** → **User and permissions**
2. Click **Invite user** and add the service account email
3. Grant role: **Release Manager** (allows app uploads)
4. Click **Invite user** to confirm

### Step 3: Run Publish Script

#### Option A: Publish to Internal Testing (Recommended First)
```bash
python publish.py --track internal
```

#### Option B: Publish to Closed Testing
```bash
python publish.py --track alpha
```

#### Option C: Publish to Open Testing
```bash
python publish.py --track beta
```

#### Option D: Publish to Production (Live)
```bash
python publish.py --track production
```

## Usage

After first upload to internal track, you can automate via CI/CD. Example script:

```bash
#!/bin/bash
# Build release bundle
./gradlew clean bundleRelease -x test

# Upload to internal testing (daily automated)
python publish.py --track internal

# Or upload to production after testing
# python publish.py --track production
```

## Safety Notes

⚠️ **Keep `play-console-credentials.json` secret!**
- Add to `.gitignore` (already done)
- Never commit to version control
- Restrict file permissions: `chmod 600 play-console-credentials.json`

## Troubleshooting

**"play-console-credentials.json not found"**
- Ensure JSON key is in project root
- Verify filename matches exactly

**"Permission denied" error**
- Check service account has "Release Manager" role in Play Console
- May take 30min for permissions to sync

**"Build failed"**
- Ensure `./gradlew clean bundleRelease -x test` succeeds first
- Check AAB path: `app/build/outputs/bundle/release/app-release.aab`

## Quick Start Summary

```bash
# 1. Download JSON key from Play Console → Settings → API access
# 2. Save as play-console-credentials.json in project root
# 3. Grant Release Manager role to service account email
# 4. Run:

python publish.py --track internal
```

Done! Your app will upload automatically. 🎉
