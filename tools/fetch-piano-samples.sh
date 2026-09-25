#!/usr/bin/env bash
# Rebuilds app/src/main/assets/piano/ from the Salamander Grand Piano V3 by Alexander Holm
# (CC BY 3.0), as packaged by FreePats: velocity layer 8 of the 30 keys it samples, a minor
# third apart from A0 to C8, mixed to mono, cut to 4.5 s with a 0.5 s fade-out, and stored
# as 16-bit WAV at 48 kHz. Needs curl, tar, shasum and ffmpeg. Downloads 742 MB once.
set -euo pipefail

url="https://freepats.zenvoid.org/Piano/SalamanderGrandPiano/SalamanderGrandPiano-SFZ+FLAC-V3+20200602.tar.gz"
archive_sha256="b7760e168494cf095344e217b0af013fc449ad033abbbdf1c65211cf11dc038b"
folder="SalamanderGrandPiano-SFZ+FLAC-V3+20200602/samples"

root="$(cd "$(dirname "$0")/.." && pwd)"
work="${TMPDIR:-/tmp}/soundcheck-salamander"
archive="$work/salamander.tar.gz"
out="$root/app/src/main/assets/piano"
mkdir -p "$work" "$out"

verified() { echo "$archive_sha256  $archive" | shasum -a 256 -c --status; }
if [ ! -f "$archive" ] || ! verified; then
    curl -fL --retry 3 -o "$archive" "$url"
fi
verified || { echo "Checksum mismatch for $archive" >&2; exit 1; }

# Salamander names each key by note and octave; the app names each file by MIDI number.
members=()
targets=()
midi=21
for octave in 0 1 2 3 4 5 6 7; do
    for note in A C "D#" "F#"; do
        [ "$midi" -gt 108 ] && break 2
        if [ "$note" = A ]; then number=$octave; else number=$((octave + 1)); fi
        members+=("$folder/${note}${number}v8.flac")
        targets+=("$(printf 'midi-%03d.wav' "$midi")")
        midi=$((midi + 3))
    done
done

tar -xzf "$archive" -C "$work" "${members[@]}"
for i in "${!members[@]}"; do
    ffmpeg -v error -y -i "$work/${members[$i]}" \
        -af "pan=mono|c0=0.5*c0+0.5*c1,atrim=end=4.5,afade=t=out:st=4:d=0.5" \
        -c:a pcm_s16le -map_metadata -1 -fflags +bitexact -flags:a +bitexact \
        "$out/${targets[$i]}"
done

if (cd "$out" && shasum -a 256 -c --status "$root/tools/piano-samples.sha256"); then
    echo "All ${#targets[@]} samples match tools/piano-samples.sha256."
else
    echo "Warning: the samples differ from tools/piano-samples.sha256; a different ffmpeg" >&2
    echo "version can change the bytes. Listen to them before committing." >&2
fi
