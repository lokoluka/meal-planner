#!/usr/bin/env python3
"""
Automated Google Play Console Publisher for MealPlanner
Uploads AAB to internal testing / staging / production tracks
"""

import json
import sys
import argparse
import subprocess
from pathlib import Path
from google.auth.transport.requests import Request
from google.oauth2.service_account import Credentials
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

# Configuration
PACKAGE_NAME = "com.lokosoft.mealplanner"
AAB_PATH = Path("app/build/outputs/bundle/release/app-release.aab")
CREDENTIALS_JSON = Path("play-console-credentials.json")

def build_release_aab():
    """Build the release AAB before uploading"""
    gradlew = "gradlew.bat" if sys.platform.startswith("win") else "./gradlew"
    print("🔨 Building release AAB...")
    result = subprocess.run(
        [gradlew, "clean", "bundleRelease", "-x", "test"],
        check=False
    )
    if result.returncode != 0:
        print("❌ Build failed. Fix the build errors and try again.")
        sys.exit(1)

def get_play_api_client(credentials_path):
    """Authenticate with Google Play Console API"""
    if not credentials_path.exists():
        print(f"❌ Error: {credentials_path} not found")
        print("\nTo set up automated publishing:")
        print("1. Go to Google Play Console → API access")
        print("2. Create a Service Account (if not exists)")
        print("3. Create a JSON key and save as 'play-console-credentials.json'")
        print("4. Run this script again")
        sys.exit(1)
    
    credentials = Credentials.from_service_account_file(
        str(credentials_path),
        scopes=['https://www.googleapis.com/auth/androidpublisher']
    )
    return build('androidpublisher', 'v3', credentials=credentials)

def upload_aab(service, aab_path, track="internal"):
    """Upload AAB to specified track (internal/alpha/beta/production)"""
    if not aab_path.exists():
        print(f"❌ AAB not found at {aab_path}")
        sys.exit(1)
    
    print(f"📦 Uploading {aab_path.name} to '{track}' track...")
    
    try:
        # Create edit
        edit_request = service.edits().insert(body={}, packageName=PACKAGE_NAME)
        edit_id = edit_request.execute()['id']
        print(f"✓ Edit created: {edit_id}")
        
        # Upload AAB
        media = MediaFileUpload(str(aab_path), mimetype='application/octet-stream')
        bundle_request = service.edits().bundles().upload(
            editId=edit_id,
            packageName=PACKAGE_NAME,
            media_body=media
        )
        bundle = bundle_request.execute()
        bundle_version = bundle['versionCode']
        print(f"✓ AAB uploaded (version: {bundle_version})")
        
        # Assign to track
        # Draft apps require 'draft' status until first production release
        release_status = 'draft'
        track_request = service.edits().tracks().update(
            editId=edit_id,
            packageName=PACKAGE_NAME,
            track=track,
            body={'releases': [{'versionCodes': [str(bundle_version)], 'status': release_status}]}
        )
        track_request.execute()
        print(f"✓ Assigned to '{track}' track")
        
        # Commit
        commit_request = service.edits().commit(
            editId=edit_id,
            packageName=PACKAGE_NAME
        )
        commit_request.execute()
        print(f"✓ Changes committed!")
        print(f"\n✅ Successfully published to {track} track!")
        
    except Exception as e:
        print(f"❌ Error: {e}")
        sys.exit(1)

def main():
    parser = argparse.ArgumentParser(description='Publish MealPlanner to Google Play')
    parser.add_argument('--track', default='internal', 
                       choices=['internal', 'alpha', 'beta', 'production'],
                       help='Release track (default: internal)')
    parser.add_argument('--credentials', default='play-console-credentials.json',
                       help='Path to service account JSON key')
    args = parser.parse_args()
    
    print("🚀 MealPlanner Publisher\n")
    build_release_aab()
    service = get_play_api_client(Path(args.credentials))
    upload_aab(service, AAB_PATH, args.track)

if __name__ == '__main__':
    main()
