#!/usr/bin/ruby

# Generates CDF from any column of a text file. (c) 2009 Etienne Riviere, NTNU Trondheim. etriviere@gmail.com
#
# Changelog:
# 15/01/2009 -- first version
# 16/01/2009 -- ignore comments and white lines

# todo:
# - add an option for the scale effect (i.e. CDF not smoothed, but stairs-looking)
# - add the possibility to specify operations on the columns (arithmetic) instead of a single one

require 'optparse'

options = {}
OptionParser.new do |opts|
  opts.banner = 
    "Usage: gen_cdf.rb "+
    "[-f/--file inputfile] "+
    "[-p/--pad] "+
    "[--pad_start F] "+
    "[--pad_stop F] "+
    "[--pad_inc F] "+
    "[-c/--col=I] "+
    "[-h/--help]"

  opts.on("-h", "--help", "Get some help") do
    puts opts.banner
    exit()
  end
  opts.on("-f inputfile", "--file filename", "Inputfile") do |f|
    options[:inputfile] = f
  end
  opts.on("-p", "--pad", "Allow padding") do
    options[:padding] = true
  end
  opts.on("", "--pad_start integer", "Start padding at") do |s|
    options[:start_pad] = s.to_f
  end
  opts.on("", "--pad_stop integer", "Stop padding at") do |s|
    options[:stop_pad] = s.to_f
  end
  opts.on("", "--pad_inc integer", "Increment by (at min)") do |s|
    options[:inc_pad] = s.to_f
  end
  opts.on("-c integer", "--col integer", "Column to use") do |c|
    options[:column] = c.to_i-1
  end
end.parse!

# check the arguments and apply defaults
if options[:inputfile] and !File.exists?(options[:inputfile])
  puts "File #{options[:inputfile]} does not exist"
  exit()
end
if !options[:column]
  options[:column]=0 # first column is the default
end
if options[:padding] and !options[:inc_pad]
  options[:inc_pad] = 1
end

# construct the set of elements
$min=nil
$max=nil
$elements={}

def process_elem(line, col)
  num=line.split(' ')[col].to_f
  # check if this is the new min
  if !$min or ($min and num < $min)
    $min = num
  end
  # check if this is the new $max
  if !$max or ($max and num > $max)
    $max = num
  end
  # increment number of occurences of the element
  if $elements[num]
    $elements[num] = $elements[num]+1
  else
    $elements[num]=1
  end
  return
end

if options[:inputfile]
  File.open(options[:inputfile]).each do |line|
    if !(line =~ /(^[[:space:]]*#|^[[:space:]]*$)/)
      process_elem(line,options[:column])
    end
  end
else
  STDIN.readlines.each do |line|
    if !(line =~ /(^[[:space:]]*#|^[[:space:]]*$)/)
      process_elem(line,options[:column])
    end
  end
end

# print the distribution

# where to start?
last=$min
if options[:padding] and options[:start_pad]
  if $min > options[:start_pad]
    last = options[:start_pad]
    $elements[options[:start_pad]] = 0
  end
end
# if inc_stop is over the current max, insert it into the elements
if options[:padding] and options[:stop_pad]
  if $max < options[:stop_pad]
    $elements[options[:stop_pad]] = 0
  end
end

total=0
$elements.each do |elem|
  total = total + elem[1]
end

sum=0
$elements.sort{|x,y| x[0]<=>y[0]}.each do |elem|
  # pad
  if options[:padding]
    while last < elem[0]
      puts "#{last} 0 #{sum} #{total}"
      last = last + options[:inc_pad]
    end
  end
  sum = sum+elem[1]
  puts "#{elem[0]} #{elem[1]} #{sum} #{total} "+
       "#{(1.0*elem[1])/(1.0*total)} #{(1.0*sum)/(1.0*total)}"
  last = elem[0]
  if options[:padding]
    last = last + options[:inc_pad]
  end
end

