import time

def onMult(m_ar,m_br):
    pha = [0.0] * m_ar * m_ar
    phb = [0.0] * m_ar * m_ar
    phc = [0.0] * m_ar * m_ar
        
    for i in range(m_ar):
        for j in range(m_ar):
            pha[i*m_ar+j] = 1.0
            
    for i in range(m_br):
        for j in range(m_br):
            phb[i*m_br+j] = 1.0
                
    start = time.time()
    
    for i in range(m_ar):
        for j in range(m_br):
            temp = 0.0
            for k in range(m_ar):
                temp += pha[i*m_ar+k] * phb[k*m_br+j]
            phc[i*m_ar+j] = temp
    
            
    end = time.time()

    print("Time:", end-start, "seconds")
    print("Result matrix:", phc[:10])
    
    

def main():
    
    print("1. Matrix Multiplication")
    print("2. Line Multiplication")
    print("3. Block Multiplication")
    
    op = int(input("Selection?: "))
    
    lin = int(input("Dimensions: lins=cols ? "))
    col = lin
    
    match op:
        case 1:
            onMult(lin, col)            
        case 2:
            print("Line Multiplication")
        case 3:
            print("Block Multiplication")
        case _:
            print("Invalid selection")
 
    
if __name__ == "__main__":
    main()