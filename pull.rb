if !ARGV[0] || !ARGV[1]
	puts "ERROR: missing parameters."
	puts "How to use:"
	puts '-' * 20
	puts "ruby pull.rb <package.name.of.your.app> <folder_to_store_pulled_data>"
	puts '-' * 20
	exit 1
end

puts '-' * 20
puts 'Script to pull ALL files and folders of an Android app'
puts '*** The app must NOT BE RUNNING (even in background) when you run this script. Be sure to kill app before you do.'
puts '-' * 20
puts

PACKAGE=ARGV[0]
ROOT="/data/data/#{PACKAGE}"
SAVE_TO=ARGV[1]

def is_dir?(path)
	abs_path = "#{ROOT}/#{path}"
	ret = `adb shell "run-as #{PACKAGE} ls #{abs_path}"`
	ret = ret.strip!
	ret != abs_path
end

def pull(path, level)
	ret = `adb shell "run-as #{PACKAGE} ls #{ROOT}/#{path}"`
	ret.strip!
	tokens = ret.split("\n")
	tokens.each do |t|
		t.strip!
		sub_path = path.empty? ? t : "#{path}/#{t}"
		indent = '    ' * level
		if is_dir?(sub_path)
			puts "#{indent}#{t}/"
			`adb shell "run-as #{PACKAGE} chmod 777 #{ROOT}/#{sub_path}"`
			pull sub_path, level + 1
		else
			save_to_path = "#{SAVE_TO}/#{path}"
			print "#{indent}#{t}\t\t\t"
			`mkdir -p #{save_to_path}`
			`adb shell "run-as #{PACKAGE} chmod 777 #{ROOT}/#{sub_path}"`
			`adb pull #{ROOT}/#{sub_path} #{save_to_path}`
		end
	end
end

`rm -rf #{SAVE_TO}`
puts "Remove \"#{SAVE_TO}\""
`mkdir -p #{SAVE_TO}`
puts "Create \"#{SAVE_TO}\""
pull '', 0
exit 0