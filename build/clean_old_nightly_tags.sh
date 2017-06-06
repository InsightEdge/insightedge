#/bin/bash
read -d '' awkScript << 'EOF'
/[0-9]+ days ago\\ +\\(tag:.*-NIGHTLY-([a-zA-Z]+)\\)$/ {
  if ( 4 < $1 ){
      sub(/\\)/, "", $5)
      print "delete tag " $5 " from " $1 " " $2 " " $3
      delete_local_tag_cmd="git tag --delete " $5
      print "executing", delete_local_tag_cmd
      system(delete_local_tag_cmd)
      delete_remote_tag_cmd="git push origin :refs/tags/"$5
      print "executing", delete_remote_tag_cmd
      system(delete_remote_tag_cmd)
  }
}
/[0-9]+ weeks ago\\ +\\(tag:.*-NIGHTLY-([a-zA-Z]+)\\)$/ {
      sub(/\\)/, "", $5)
      print "delete tag " $5 " from " $1 " " $2 " " $3
      delete_local_tag_cmd="git tag --delete " $5
      print "executing", delete_local_tag_cmd
      system(delete_local_tag_cmd)
      delete_remote_tag_cmd="git push origin :refs/tags/"$5
      print "executing", delete_remote_tag_cmd
      system(delete_remote_tag_cmd)
}

EOF

git log --tags --simplify-by-decoration --pretty="format:%ar %d" | awk "$awkScript"

