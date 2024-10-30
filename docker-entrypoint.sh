function fixDirectoryRights() {
  find "$1" \! -user "$2" -exec chown "$2:$2" '{}' +
}

echo "Running Starter Script as User: $(whoami)"

if [ "$EUID" -eq 0 ]
  then
    echo "Fixing File System Rights"
    fixDirectoryRights "/home/spring/logs" "spring"
fi

exec gosu spring $@
exit 0;