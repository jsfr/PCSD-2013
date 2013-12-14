graphics_toolkit("fltk");
A = load('log.txt');
B = [];
C = [];

for j=5:5:A(end,1),
    B(end+1,:) = mean(A(A(:,1)==j,:));
    C(end+1,:) = std(A(A(:,1)==j,:));
end

title('Throughput');
xlabel('# of workers');
ylabel('succXact / s');
plot(B(:,1),B(:,2));
print('-dpng','throughput.png');

title('Latency');
xlabel('# of workers');
ylabel('seconds');
plot(B(:,1),B(:,3));
print('-dpng','latency.png');