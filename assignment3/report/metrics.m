graphics_toolkit("fltk");

function [] = metrics()
  A = load('benchmark.local.dat')
  B = load('benchmark.remote.dat')
  generate_graph(A,'local');
  generate_graph(B,'remote');
end

function [] = generate_graph(A,txt)
  B = [];
  C = [];

  for j=1:1:A(end,1),
      B(end+1,:) = mean(A(A(:,1)==j,:));
      C(end+1,:) = std(A(A(:,1)==j,:));
  end

  title('Throughput');
  xlabel('# of workers');
  ylabel('succXact / s');
  plot(B(:,1),B(:,2));
  print('-dpng','throughput-' ,txt ,'.png');

  title('Latency');
  xlabel('# of workers');
  ylabel('seconds');
  plot(B(:,1),B(:,3));
  print('-dpng','latency-',txt,'.png');
end

metrics()
